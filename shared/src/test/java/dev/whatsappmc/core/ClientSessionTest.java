package dev.whatsappmc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientSessionTest {
	@Test
	void hintIsShownOnceAndTheSettingsScreenOpensFromTheKey(@TempDir Path dir) {
		FakeOutput output = new FakeOutput();
		output.ready = true;
		ClientSession session = new ClientSession(Settings.load(dir.resolve("config.json")), output);
		List<Boolean> opened = new ArrayList<>();

		session.tick(false, false, () -> opened.add(true));
		session.tick(false, false, () -> opened.add(true));
		assertEquals(List.of("status:" + SettingsForm.NO_CONTACT_HINT), output.lines);
		assertTrue(opened.isEmpty());

		session.tick(true, false, () -> opened.add(true));
		assertEquals(List.of(true), opened);

		session.tick(true, true, () -> opened.add(false));
		assertEquals(List.of(true), opened);
	}

	@Test
	void rejectedContactIsNotWritten(@TempDir Path dir) {
		Path file = dir.resolve("config.json");
		SettingsForm.SaveResult result = SettingsForm.save(file, Modes.LITE, "Ada", "31600000000", "/nope", "");
		assertFalse(result.saved());
		assertFalse(Files.exists(file));
	}

	@Test
	void savedContactCanBeReloaded(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("config.json");
		SettingsForm.SaveResult result = SettingsForm.save(file, Modes.LITE, "Ada", "+31 6 0000 0000", ".h ", "");
		assertTrue(result.saved());
		assertEquals(SettingsForm.SAVED, result.notice());
		Settings loaded = Settings.load(file);
		assertEquals("31600000000", loaded.allowedNumber);
		assertEquals("Ada", loaded.displayName);
		assertTrue(loaded.token.length() >= 32);
		assertEquals(Modes.LITE, loaded.mode);
	}

	@Test
	void fullModeKeepsThePrivateNumberAndDoesNotSwallowChat(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("config.json");
		Settings lite = Settings.saveSelection(file, Modes.LITE, "Ada", "+31 6 0000 0000", ".h ", "");
		Settings full = Settings.saveSelection(file, Modes.FULL, lite.displayName, lite.allowedNumber, lite.prefix, "Cave night");
		assertEquals("31600000000", full.allowedNumber);
		assertEquals("Cave night", full.groupName);
		assertEquals(lite.token, full.token);
		WhatsAppRuntime runtime = WhatsAppRuntime.start(full, new FakeOutput());
		assertTrue(runtime.allowChat(".h this stays in Minecraft"));
	}
}
