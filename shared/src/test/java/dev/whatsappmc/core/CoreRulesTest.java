package dev.whatsappmc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class CoreRulesTest {
	@Test
	void prefixSwallowsOnlyAMatchingReply() {
		assertTrue(Prefix.shouldSwallow(".h hello", ".h "));
		assertFalse(Prefix.shouldSwallow("hello", ".h "));
		assertFalse(Prefix.shouldSwallow("/whatsapp", ".h "));
		assertFalse(Prefix.valid("/h "));
		assertEquals("hello", Prefix.body(".h hello", ".h "));
	}

	@Test
	void sanitizerDropsControlsAndSectionSigns() {
		assertEquals("hello there", Sanitizer.sanitize(" \nhello\tthere\u0000\u00A7 ", 100));
		List<String> parts = Sanitizer.chunks("abcdef", 2);
		assertEquals(List.of("ab", "cd", "ef"), parts);
	}

	@Test
	void phoneNumbersKeepInternationalDigits() {
		assertEquals("31600000000", PhoneNumbers.digits("+31 6 0000 0000"));
		assertTrue(PhoneNumbers.valid("31600000000"));
		assertFalse(PhoneNumbers.valid("123"));
		assertEquals("31600000000", PhoneNumbers.digits("0031600000000"));
	}

	@Test
	void framesRejectGarbageAndAcceptAuth() {
		assertTrue(Frames.parse("{").isEmpty());
		assertTrue(Frames.parse("{\"v\":2,\"type\":\"in\"}").isEmpty());
		Frame frame = Frames.parse(Frames.auth("abc")).orElseThrow();
		assertEquals("auth", frame.type);
		assertEquals("abc", frame.token);
		assertFalse(TokenCheck.equals("abc", "abd"));
		assertTrue(TokenCheck.equals("same-token", "same-token"));
	}
}
