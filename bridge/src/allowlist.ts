import { digits, validPhone } from './phone.js'

export type AddressKey = {
  remoteJid?: string | null
  remoteJidAlt?: string | null
  senderPn?: string | null
}

export type Decision = {
  accept: boolean
  replyJid?: string
  pairLid?: string
}

export function decide(allowedDigits: string, knownLids: ReadonlySet<string>, key: AddressKey): Decision {
  const remote = key.remoteJid ?? ''
  if (!remote || isIgnored(remote) || !validPhone(allowedDigits)) return { accept: false }
  const lid = remote.endsWith('@lid') ? remote : undefined
  const phones = [phoneOf(key.remoteJid), phoneOf(key.remoteJidAlt), phoneOf(key.senderPn)]
  const phoneMatch = phones.some((value) => value === allowedDigits)
  const lidKnown = lid !== undefined && knownLids.has(lid)
  if (!phoneMatch && !lidKnown) return { accept: false }
  return { accept: true, replyJid: remote, pairLid: phoneMatch ? lid : undefined }
}

/** Exact group subject once, then the saved @g.us id. A later rename does not retarget a different group. */
export function decideGroup(groupName: string, knownJid: string, remoteJid: string, subject: string): Decision & { bind: boolean } {
  const name = groupName.trim()
  if (!remoteJid.endsWith('@g.us') || !name) return { accept: false, bind: false }
  if (knownJid && remoteJid === knownJid) return { accept: true, replyJid: remoteJid, bind: false }
  if (subject.trim() === name) return { accept: true, replyJid: remoteJid, bind: true }
  return { accept: false, bind: false }
}

function isIgnored(jid: string): boolean {
  return jid.endsWith('@g.us') || jid.includes('@broadcast') || jid.endsWith('@newsletter') || jid === 'status@broadcast'
}

function phoneOf(value: string | null | undefined): string {
  if (!value || value.endsWith('@lid') || isIgnored(value)) return ''
  const user = value.split('@')[0]?.split(':')[0] ?? ''
  const parsed = digits(user)
  return validPhone(parsed) ? parsed : ''
}
