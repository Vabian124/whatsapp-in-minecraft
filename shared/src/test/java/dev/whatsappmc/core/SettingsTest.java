package dev.whatsappmc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsTest {
	@Test
	void saveKeepsTokenAndStoresDigits(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("config.json");
		Settings first = Settings.saveContact(file, "Ada", "+31 6 0000 0000", ".h ");
		Settings second = Settings.saveContact(file, "Bea", "31611111111", ".w ");
		assertEquals(first.token, second.token);
		assertEquals("31611111111", second.allowedNumber);
		assertEquals(".w ", second.prefix);
		assertTrue(second.hasContact());
		assertNull(second.problem);
		assertTrue(Files.readString(file).contains("\"token\""));
	}

	@Test
	void missingFileDoesNotConnect(@TempDir Path dir) {
		Settings settings = Settings.load(dir.resolve("config.json"));
		assertFalse(settings.canConnect());
	}
}
