import assert from 'node:assert/strict'
import { once } from 'node:events'
import test from 'node:test'
import { WebSocket } from 'ws'
import { frame } from './protocol.js'
import { startServer, type GameServer } from './server.js'

const TOKEN = 'a'.repeat(32)

async function openBridge(): Promise<{ game: GameServer; port: number }> {
  const game = startServer({
    port: 0,
    token: TOKEN,
    onOutgoing: (socket, id) => {
      socket.send(frame({ type: 'nack', id }))
    },
  })
  return { game, port: await game.ready }
}

function textOf(raw: unknown): string {
  if (typeof raw === 'string') return raw
  if (raw instanceof Buffer) return raw.toString('utf8')
  return String(raw)
}

test('an authenticated local client gets a dry reply without WhatsApp', async () => {
  const { game, port } = await openBridge()
  const socket = new WebSocket(`ws://127.0.0.1:${port}`)
  try {
    await once(socket, 'open')
    const reply = once(socket, 'message')
    socket.send(frame({ type: 'auth', token: TOKEN }))
    socket.send(frame({ type: 'out', id: 'c1', text: 'hi' }))
    const [raw] = await reply
    assert.equal(textOf(raw), frame({ type: 'nack', id: 'c1' }))
  } finally {
    socket.close()
    await game.close()
  }
})

test('a wrong token is closed before any reply', async () => {
  const { game, port } = await openBridge()
  const socket = new WebSocket(`ws://127.0.0.1:${port}`)
  const closed = once(socket, 'close')
  try {
    await once(socket, 'open')
    socket.send(frame({ type: 'auth', token: 'nope' }))
    await closed
  } finally {
    socket.close()
    await game.close()
  }
})

test('garbage is closed before it is treated as a frame', async () => {
  const { game, port } = await openBridge()
  const socket = new WebSocket(`ws://127.0.0.1:${port}`)
  const closed = once(socket, 'close')
  try {
    await once(socket, 'open')
    socket.send('{')
    await closed
  } finally {
    socket.close()
    await game.close()
  }
})
