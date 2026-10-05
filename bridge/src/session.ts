import makeWASocket, {
  Browsers,
  DisconnectReason,
  fetchLatestBaileysVersion,
  makeCacheableSignalKeyStore,
  useMultiFileAuthState,
  type WASocket,
} from '@whiskeysockets/baileys'
import pino from 'pino'
import qrcode from 'qrcode-terminal'
import { decide, decideGroup } from './allowlist.js'
import { extractBody } from './extract.js'

export type LinkState = 'qr' | 'open' | 'close'
export type Incoming = { id: string; text: string; kind: 'text' | 'media'; lane: 'direct' | 'group'; sender: string }

export type Session = {
  send(jid: string, text: string): Promise<void>
  close(): void
}

const seen = new Set<string>()

export async function startSession(options: {
  authDir: string
  startedAt: number
  allowedNumber: () => string
  groupName: () => string
  knownLids: () => ReadonlySet<string>
  knownGroupJid: () => string
  onPair: (lid: string) => void
  onBindGroup: (jid: string) => void
  onReplyJid: (jid: string) => void
  onMessage: (message: Incoming) => void
  onState: (state: LinkState) => void
}): Promise<Session> {
  let generation = 0
  let current: WASocket | undefined
  let reconnectTimer: NodeJS.Timeout | undefined
  let stopped = false

  async function connect(): Promise<void> {
    if (stopped) return
    const generationId = ++generation
    const { state, saveCreds } = await useMultiFileAuthState(options.authDir)
    const { version } = await fetchLatestBaileysVersion()
    const logger = pino({ level: 'silent' })
    const sock = makeWASocket({
      version,
      auth: { creds: state.creds, keys: makeCacheableSignalKeyStore(state.keys, logger) },
      logger,
      browser: Browsers.windows('Desktop'),
      syncFullHistory: false,
      shouldSyncHistoryMessage: () => false,
      markOnlineOnConnect: false,
      generateHighQualityLinkPreview: false,
    })
    current = sock
    sock.ev.on('creds.update', saveCreds)
    sock.ev.on('connection.update', (update) => {
      if (generationId !== generation) return
      if (update.qr) {
        qrcode.generate(update.qr, { small: true })
        console.log('Scan the QR code above in WhatsApp: Linked devices.')
        options.onState('qr')
      }
      if (update.connection === 'open') {
        console.log('WhatsApp linked.')
        options.onState('open')
      }
      if (update.connection === 'close') {
        options.onState('close')
        const code = (update.lastDisconnect?.error as { output?: { statusCode?: number } } | undefined)?.output?.statusCode
        if (code === DisconnectReason.loggedOut) {
          console.log('This linked device was logged out. Close the bridge, delete the auth folder, and start again.')
          return
        }
        if (!reconnectTimer && !stopped) {
          reconnectTimer = setTimeout(() => {
            reconnectTimer = undefined
            if (generationId === generation) void connect()
          }, 2000)
        }
      }
    })
    sock.ev.on('messages.upsert', ({ messages, type }) => {
      if (type !== 'notify' || generationId !== generation) return
      for (const message of messages) accept(message, generationId)
    })
  }

  function accept(message: { key: { id?: string | null; fromMe?: boolean | null; remoteJid?: string | null; remoteJidAlt?: string | null }; message?: unknown; messageTimestamp?: unknown; pushName?: string | null }, generationId: number): void {
    if (message.key.fromMe || !message.key.id || !remember(message.key.id)) return
    const remote = message.key.remoteJid ?? ''
    if (remote.endsWith('@g.us')) {
      void acceptGroup(message, generationId)
      return
    }
    const when = timestampMs(message.messageTimestamp)
    if (when < options.startedAt - 5000) return
    const decision = decide(options.allowedNumber(), options.knownLids(), {
      remoteJid: message.key.remoteJid,
      remoteJidAlt: message.key.remoteJidAlt,
      senderPn: (message.key as { senderPn?: string | null }).senderPn,
    })
    if (!decision.accept || !decision.replyJid) return
    if (decision.pairLid) options.onPair(decision.pairLid)
    options.onReplyJid(decision.replyJid)
    const body = extractBody(message.message)
    if (!body) return
    options.onMessage({ id: message.key.id, kind: body.kind, text: body.text.slice(0, 4000), lane: 'direct', sender: message.pushName || 'WhatsApp' })
  }

  // Groups are Full mode. The first exact subject match is stored, so two groups
  // with the same name are not a coin flip after that.
  async function acceptGroup(message: { key: { id?: string | null; remoteJid?: string | null }; message?: unknown; messageTimestamp?: unknown; pushName?: string | null }, generationId: number): Promise<void> {
    if (generationId !== generation) return
    const remote = message.key.remoteJid ?? ''
    const when = timestampMs(message.messageTimestamp)
    if (when < options.startedAt - 5000) return
    const known = options.knownGroupJid()
    let subject = ''
    if (known !== remote) {
      try {
        subject = (await current?.groupMetadata(remote))?.subject ?? ''
      } catch {
        return
      }
    }
    if (generationId !== generation) return
    const decision = decideGroup(options.groupName(), known, remote, subject)
    if (!decision.accept || !decision.replyJid) return
    if (decision.bind) options.onBindGroup(decision.replyJid)
    const body = extractBody(message.message)
    if (!body || !message.key.id) return
    options.onMessage({ id: message.key.id, kind: body.kind, text: body.text.slice(0, 4000), lane: 'group', sender: message.pushName || 'WhatsApp' })
  }

  await connect()
  return {
    async send(jid, text) {
      if (!current) throw new Error('not linked')
      await current.sendMessage(jid, { text })
    },
    close() {
      stopped = true
      generation++
      if (reconnectTimer) clearTimeout(reconnectTimer)
      current?.end(undefined)
    },
  }
}

function remember(id: string): boolean {
  if (seen.has(id)) return false
  seen.add(id)
  if (seen.size > 500) {
    const oldest = seen.values().next().value
    if (oldest) seen.delete(oldest)
  }
  return true
}

function timestampMs(value: unknown): number {
  if (typeof value === 'number') return value * 1000
  if (typeof value === 'object' && value && 'toNumber' in value && typeof value.toNumber === 'function') {
    return value.toNumber() * 1000
  }
  return 0
}
