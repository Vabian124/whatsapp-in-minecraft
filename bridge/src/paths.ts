import os from 'node:os'
import path from 'node:path'

export function dataDir(): string {
  if (process.platform === 'win32' && process.env.APPDATA) {
    return path.join(process.env.APPDATA, 'whatsapp-mc')
  }
  return path.join(os.homedir(), '.config', 'whatsapp-mc')
}
