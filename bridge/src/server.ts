import type { WebSocket } from 'ws'
import { WebSocketServer } from 'ws'
import { frame, parseFrame, type Frame } from './protocol.js'
import { isLoopback, tokenEquals } from './token.js'

export type GameServer = {
  ready: Promise<number>
  broadcast(body: Frame): void
  close(): Promise<void>
}

export function startServer(options: {
  port: number
  token: string
  onOutgoing: (socket: WebSocket, id: string, text: string, lane: string) => void
}): GameServer {
  const sockets = new Set<WebSocket>()
  const server = new WebSocketServer({ host: '127.0.0.1', port: options.port })
  const ready = boundPort(server)
  server.on('connection', (socket, request) => {
    if (!isLoopback(request.socket.remoteAddress)) {
      socket.close()
      return
    }
    let authed = false
    const timer = setTimeout(() => {
      if (!authed) socket.close()
    }, 2000)
    socket.on('message', (data) => {
      const raw = typeof data === 'string' ? data : data.toString()
      const parsed = parseFrame(raw)
      if (!parsed) {
        socket.close()
        return
      }
      if (!authed) {
        if (parsed.type !== 'auth' || typeof parsed.token !== 'string' || !tokenEquals(parsed.token, options.token)) {
          socket.close()
          return
        }
        authed = true
        clearTimeout(timer)
        sockets.add(socket)
        return
      }
      if (parsed.type === 'out' && parsed.id && typeof parsed.text === 'string') {
        options.onOutgoing(socket, parsed.id, parsed.text, parsed.lane === 'group' ? 'group' : 'direct')
      }
    })
    socket.on('close', () => {
      clearTimeout(timer)
      sockets.delete(socket)
    })
    socket.on('error', () => socket.close())
  })
  return {
    ready,
    broadcast(body) {
      const raw = frame(body)
      for (const socket of sockets) {
        if (socket.readyState === socket.OPEN) socket.send(raw)
      }
    },
    close() {
      for (const socket of sockets) socket.close()
      return new Promise((resolve) => server.close(() => resolve()))
    },
  }
}

function boundPort(server: WebSocketServer): Promise<number> {
  const current = server.address()
  if (current && typeof current === 'object') return Promise.resolve(current.port)
  return new Promise((resolve, reject) => {
    server.once('listening', () => {
      const address = server.address()
      if (address && typeof address === 'object') resolve(address.port)
      else reject(new Error('Bridge did not bind a port.'))
    })
    server.once('error', reject)
  })
}
