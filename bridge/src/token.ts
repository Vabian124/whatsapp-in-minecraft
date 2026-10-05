import { timingSafeEqual } from 'node:crypto'

export function tokenEquals(left: string, right: string): boolean {
  const a = Buffer.from(left)
  const b = Buffer.from(right)
  if (a.length !== b.length) {
    timingSafeEqual(a, a)
    return false
  }
  return timingSafeEqual(a, b)
}

export function isLoopback(address: string | undefined): boolean {
  if (!address) return false
  return address === '127.0.0.1' || address === '::1' || address.startsWith('::ffff:127.')
}
