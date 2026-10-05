export function digits(raw: string | undefined | null): string {
  if (!raw) return ''
  const only = raw.replace(/\D/g, '')
  return only.startsWith('00') ? only.slice(2) : only
}

export function validPhone(value: string): boolean {
  return value.length >= 8 && value.length <= 15
}
