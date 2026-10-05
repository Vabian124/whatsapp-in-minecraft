import { readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { watch } from 'node:fs'
import { pathToFileURL } from 'node:url'
import type { WebSocket } from 'ws'
import { loadConfig, restrictDir, type BridgeConfig } from './config.js'
import { dataDir } from './paths.js'
import { MAX_QUEUE, MAX_TEXT } from './limits.js'
import { frame } from './protocol.js'
import { startServer } from './server.js'
import { startSession, type LinkState } from './session.js'

const startedAt = Date.now()
const dir = dataDir()
const lids = new Set<string>()
let config: BridgeConfig
let replyJid = ''
let groupJid = ''
let displayName = ''
let link: LinkState | '' = ''

const queue: Array<{ socket: WebSocket; id: string; text: string; jid: string }> = []
let pumping = false
let lastSent = 0

async function main(): Promise<void> {
  await restrictDir(dir)
  config = await loadConfig(dir)
  displayName = config.displayName || 'WhatsApp'
  await loadLids()
  groupJid = await loadGroup()
  const active: { session?: Awaited<ReturnType<typeof startSession>> } = {}
  const server = startServer({
    port: config.port,
    token: config.token,
    onOutgoing: (socket, id, text, lane) => {
      const jid = lane === 'group' ? groupJid : replyJid
      if (!jid) {
        socket.send(frame({ type: 'nack', id }))
        return
      }
      if (queue.length >= MAX_QUEUE) {
        socket.send(frame({ type: 'nack', id }))
        return
      }
      queue.push({ socket, id, text: text.slice(0, MAX_TEXT), jid })
      void pump()
    },
  })
  const session = await startSession({
    authDir: path.join(dir, 'auth'),
    startedAt,
    allowedNumber: () => config.allowedNumber,
    groupName: () => config.groupName,
    knownLids: () => lids,
    knownGroupJid: () => groupJid,
    onPair: (lid) => {
      if (lids.has(lid)) return
      lids.add(lid)
      void saveLids()
    },
    onBindGroup: (jid) => {
      if (groupJid === jid) return
      groupJid = jid
      void saveGroup()
    },
    onReplyJid: (jid) => {
      replyJid = jid
    },
    onMessage: (message) => {
      if (message.lane === 'group' && message.text.trim() === '.mc players') {
        server.broadcast({ type: 'ask', lane: 'group', id: message.id, text: 'players' })
        return
      }
      if (message.lane === 'group' && message.text.trim() === '.mc help') {
        if (groupJid) void active.session?.send(groupJid, 'Commands: .mc players, .mc help')
        return
      }
      const name = message.lane === 'group' ? message.sender : displayName
      server.broadcast({ type: 'in', lane: message.lane, id: message.id, name, text: message.text, kind: message.kind })
    },
    onState: (state) => {
      if (link === state) return
      link = state
      server.broadcast({ type: 'status', state })
    },
  })
  active.session = session

  let reloadTimer: NodeJS.Timeout | undefined
  watch(dir, (_event, filename) => {
    if (filename !== 'config.json') return
    if (reloadTimer) clearTimeout(reloadTimer)
    reloadTimer = setTimeout(() => void reloadConfig(), 200)
  })

  async function pump(): Promise<void> {
    if (pumping) return
    pumping = true
    try {
      while (queue.length > 0) {
        const wait = 1000 - (Date.now() - lastSent)
        if (wait > 0) await delay(wait)
        const job = queue.shift()
        if (!job || job.socket.readyState !== job.socket.OPEN) continue
        try {
          await session.send(job.jid, job.text)
          lastSent = Date.now()
          job.socket.send(frame({ type: 'ack', id: job.id }))
        } catch {
          job.socket.send(frame({ type: 'nack', id: job.id }))
        }
      }
    } finally {
      pumping = false
    }
  }

  const shutdown = () => {
    session.close()
    void server.close().then(() => process.exit(0))
  }
  process.on('SIGINT', shutdown)
  process.on('SIGTERM', shutdown)
  console.log(`Bridge ready on 127.0.0.1:${config.port}. Config: ${path.join(dir, 'config.json')}`)
  if (!config.allowedNumber && config.mode !== 'full') console.log('No contact yet. In Minecraft, type /whatsapp and save a phone number.')
  if (config.mode === 'full' && !config.groupName) console.log('Full mode is on. Set the WhatsApp group name in the mod screen.')
}

async function reloadConfig(): Promise<void> {
  try {
    const next = await loadConfig(dir)
    if (next.token !== config.token || next.port !== config.port) {
      console.log('Port or token changed. Restart the bridge to apply that.')
    }
    if (next.allowedNumber !== config.allowedNumber) {
      lids.clear()
      replyJid = ''
      await saveLids()
      console.log(next.allowedNumber ? 'Contact updated.' : 'Contact cleared.')
    }
    if (next.groupName !== config.groupName) {
      groupJid = ''
      await saveGroup()
      console.log(next.groupName ? 'Group updated.' : 'Group cleared.')
    }
    displayName = next.displayName || 'WhatsApp'
    config = next
  } catch {
    console.log('Config is being saved. It will be read again when the file is complete.')
  }
}

async function loadLids(): Promise<void> {
  try {
    const parsed = JSON.parse(await readFile(path.join(dir, 'lids.json'), 'utf8')) as unknown
    if (!Array.isArray(parsed)) return
    for (const value of parsed) {
      if (typeof value === 'string' && value.endsWith('@lid')) lids.add(value)
    }
  } catch {
    // No paired ids yet.
  }
}

async function saveLids(): Promise<void> {
  await writeFile(path.join(dir, 'lids.json'), JSON.stringify([...lids]) + '\n', 'utf8')
}

async function loadGroup(): Promise<string> {
  try {
    const parsed = JSON.parse(await readFile(path.join(dir, 'group.json'), 'utf8')) as { jid?: unknown }
    return typeof parsed.jid === 'string' && parsed.jid.endsWith('@g.us') ? parsed.jid : ''
  } catch {
    return ''
  }
}

async function saveGroup(): Promise<void> {
  await writeFile(path.join(dir, 'group.json'), JSON.stringify({ jid: groupJid }) + '\n', 'utf8')
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

const entry = process.argv[1]
const launched = entry ? pathToFileURL(path.resolve(entry)).href : ''
if (launched === import.meta.url) {
  main().catch((error: unknown) => {
    console.log(error instanceof Error ? error.message : 'Bridge failed to start.')
    process.exit(1)
  })
}
