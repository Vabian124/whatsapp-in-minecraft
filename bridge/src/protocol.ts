export type Frame = {
  v?: number
  type?: string
  token?: string
  state?: string
  id?: string
  text?: string
  kind?: string
  name?: string
  lane?: string
}

export function parseFrame(raw: string): Frame | null {
  if (raw.length === 0 || raw.length > 8192) return null
  let value: unknown
  try {
    value = JSON.parse(raw)
  } catch {
    return null
  }
  if (!value || typeof value !== 'object' || Array.isArray(value)) return null
  const frame = value as Frame
  if (frame.v !== 1 || typeof frame.type !== 'string' || frame.type.length === 0 || frame.type.length > 32) return null
  if (frame.text !== undefined && (typeof frame.text !== 'string' || frame.text.length > 4200)) return null
  if (frame.token !== undefined && (typeof frame.token !== 'string' || frame.token.length > 256)) return null
  if (frame.id !== undefined && (typeof frame.id !== 'string' || frame.id.length > 64)) return null
  if (frame.name !== undefined && (typeof frame.name !== 'string' || frame.name.length > 64)) return null
  if (frame.kind !== undefined && (typeof frame.kind !== 'string' || frame.kind.length > 16)) return null
  if (frame.state !== undefined && (typeof frame.state !== 'string' || frame.state.length > 32)) return null
  if (frame.lane !== undefined && (typeof frame.lane !== 'string' || frame.lane.length > 16)) return null
  return frame
}

export function frame(body: Frame): string {
  return JSON.stringify({ v: 1, ...body })
}
