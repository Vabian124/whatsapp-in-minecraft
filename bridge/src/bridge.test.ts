import assert from 'node:assert/strict'
import test from 'node:test'
import { decide, decideGroup } from './allowlist.js'
import { extractBody } from './extract.js'
import { digits, validPhone } from './phone.js'
import { parseFrame } from './protocol.js'
import { isLoopback, tokenEquals } from './token.js'

test('only the saved number is accepted, and a reply uses the inbound id', () => {
  const lids = new Set<string>()
  const first = decide('31600000000', lids, {
    remoteJid: '111@lid',
    remoteJidAlt: '31600000000@s.whatsapp.net',
  })
  assert.equal(first.accept, true)
  assert.equal(first.replyJid, '111@lid')
  assert.equal(first.pairLid, '111@lid')

  const known = new Set(['111@lid'])
  const later = decide('31600000000', known, { remoteJid: '111@lid' })
  assert.equal(later.accept, true)
  assert.equal(later.pairLid, undefined)

  assert.equal(decide('31600000000', known, { remoteJid: '999@lid' }).accept, false)
  assert.equal(decide('31600000000', known, { remoteJid: '31600000000-1@g.us' }).accept, false)
  assert.equal(decide('', known, { remoteJid: '31600000000@s.whatsapp.net' }).accept, false)
})

test('text and media placeholders are extracted without downloading', () => {
  assert.deepEqual(extractBody({ conversation: 'hello' }), { kind: 'text', text: 'hello' })
  assert.deepEqual(extractBody({ ephemeralMessage: { message: { imageMessage: { caption: 'look' } } } }), {
    kind: 'media',
    text: '[photo] look',
  })
  assert.equal(extractBody({ protocolMessage: { type: 0 } }), null)
})

test('a group is accepted only by its exact name, then by the saved id', () => {
  assert.equal(decideGroup('Cave night', '', '111@g.us', 'Cave night').accept, true)
  assert.equal(decideGroup('Cave night', '', '111@g.us', 'Cave night').bind, true)
  assert.equal(decideGroup('Cave night', '111@g.us', '111@g.us', '').accept, true)
  assert.equal(decideGroup('Cave night', '111@g.us', '222@g.us', 'Other').accept, false)
  assert.equal(decideGroup('', '', '111@g.us', 'Cave night').accept, false)
})

test('phone digits match the mod rules', () => {
  assert.equal(digits('+31 6 0000 0000'), '31600000000')
  assert.equal(digits('0031600000000'), '31600000000')
  assert.equal(validPhone('31600000000'), true)
  assert.equal(validPhone('123'), false)
})

test('frames and tokens fail closed', () => {
  assert.equal(parseFrame('{'), null)
  assert.equal(parseFrame(JSON.stringify({ v: 1, type: 'auth', token: 'abc' }))?.type, 'auth')
  assert.equal(tokenEquals('one', 'one'), true)
  assert.equal(tokenEquals('one', 'two'), false)
  assert.equal(isLoopback('127.0.0.1'), true)
  assert.equal(isLoopback('::ffff:127.0.0.1'), true)
  assert.equal(isLoopback('10.0.0.8'), false)
})
