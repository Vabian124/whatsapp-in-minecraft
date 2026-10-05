package dev.whatsappmc.core;

import java.nio.file.Path;

public final class BridgePaths {
	private BridgePaths() {
	}

	public static Path dir() {
		String appData = System.getenv("APPDATA");
		if (appData != null && !appData.isBlank()) {
			return Path.of(appData, "whatsapp-mc");
		}
		return Path.of(System.getProperty("user.home"), ".config", "whatsapp-mc");
	}

	public static Path configFile() {
		return dir().resolve("config.json");
	}
}
