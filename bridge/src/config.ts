import { randomBytes } from 'node:crypto'
import { execFile } from 'node:child_process'
import { mkdir, readFile, rename, writeFile, chmod } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { digits, validPhone } from './phone.js'

export type BridgeConfig = {
  mode: 'lite' | 'full'
  allowedNumber: string
  displayName: string
  prefix: string
  groupName: string
  port: number
  token: string
}

export async function loadConfig(dir: string): Promise<BridgeConfig> {
  const file = path.join(dir, 'config.json')
  await mkdir(dir, { recursive: true })
  let raw: string
  try {
    raw = await readFile(file, 'utf8')
  } catch {
    const created = fresh()
    await writeConfig(dir, created)
    return created
  }
  const parsed = JSON.parse(raw) as Partial<BridgeConfig>
  const token = typeof parsed.token === 'string' && parsed.token.length >= 32 ? parsed.token : randomBytes(32).toString('hex')
  const port = Number.isInteger(parsed.port) && (parsed.port as number) >= 1 && (parsed.port as number) <= 65535 ? parsed.port as number : 17321
  const allowedNumber = digits(typeof parsed.allowedNumber === 'string' ? parsed.allowedNumber : '')
  const displayName = cleanName(parsed.displayName)
  const prefix = typeof parsed.prefix === 'string' && parsed.prefix.length >= 2 && !parsed.prefix.startsWith('/') ? parsed.prefix : '.h '
  const groupName = cleanGroup(parsed.groupName)
  const mode: 'lite' | 'full' = parsed.mode === 'full' ? 'full' : 'lite'
  const config = { mode, allowedNumber: validPhone(allowedNumber) ? allowedNumber : '', displayName, prefix, groupName, port, token }
  if (token !== parsed.token) await writeConfig(dir, config)
  return config
}

export async function writeConfig(dir: string, config: BridgeConfig): Promise<void> {
  const file = path.join(dir, 'config.json')
  const tmp = file + '.tmp'
  await mkdir(dir, { recursive: true })
  await writeFile(tmp, JSON.stringify(config, null, 2) + '\n', 'utf8')
  await rename(tmp, file)
}

export async function restrictDir(dir: string): Promise<void> {
  if (process.platform !== 'win32') {
    await chmod(dir, 0o700)
    return
  }
  const user = os.userInfo().username
  await new Promise<void>((resolve) => {
    execFile('icacls', [dir, '/inheritance:r', '/grant:r', `${user}:(OI)(CI)F`, '/grant:r', 'SYSTEM:(OI)(CI)F'], () => resolve())
  })
}

function fresh(): BridgeConfig {
  return {
    mode: 'lite',
    allowedNumber: '',
    displayName: '',
    prefix: '.h ',
    groupName: '',
    port: 17321,
    token: randomBytes(32).toString('hex'),
  }
}

function cleanName(value: unknown): string {
  if (typeof value !== 'string') return ''
  const name = value.trim()
  return name.length > 32 ? name.slice(0, 32) : name
}

function cleanGroup(value: unknown): string {
  if (typeof value !== 'string') return ''
  const name = value.trim()
  return name.length > 64 ? name.slice(0, 64) : name
}
