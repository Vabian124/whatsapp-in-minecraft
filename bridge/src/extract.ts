export type Extracted = { kind: 'text' | 'media'; text: string }

const MEDIA: Array<[string, string]> = [
  ['imageMessage', '[photo]'],
  ['videoMessage', '[video]'],
  ['audioMessage', '[voice]'],
  ['stickerMessage', '[sticker]'],
  ['documentMessage', '[file]'],
  ['contactMessage', '[contact]'],
  ['contactsArrayMessage', '[contact]'],
  ['locationMessage', '[location]'],
  ['liveLocationMessage', '[location]'],
]

export function extractBody(message: unknown): Extracted | null {
  const node = unwrap(message)
  if (!node || typeof node !== 'object') return null
  const record = node as Record<string, unknown>
  if (typeof record.conversation === 'string') return { kind: 'text', text: record.conversation }
  const extended = record.extendedTextMessage
  if (isRecord(extended) && typeof extended.text === 'string') return { kind: 'text', text: extended.text }
  for (const [key, label] of MEDIA) {
    const part = record[key]
    if (!isRecord(part)) continue
    const caption = typeof part.caption === 'string' ? part.caption.trim() : ''
    return { kind: 'media', text: caption ? `${label} ${caption}` : label }
  }
  return null
}

function unwrap(message: unknown): unknown {
  let current = message
  for (let i = 0; i < 3; i++) {
    if (!isRecord(current)) return current
    const inner = nested(current.ephemeralMessage)
      ?? nested(current.viewOnceMessage)
      ?? nested(current.viewOnceMessageV2)
      ?? nested(current.documentWithCaptionMessage)
    if (!inner) return current
    current = inner
  }
  return current
}

function nested(value: unknown): unknown {
  if (!isRecord(value)) return undefined
  return value.message
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object'
}
