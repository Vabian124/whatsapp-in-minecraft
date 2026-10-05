package dev.whatsappmc.core;

public final class ClientSession {
	private final WhatsAppRuntime runtime;
	private final GameOutput output;
	private boolean hinted;

	public ClientSession(Settings settings, GameOutput output) {
		this.output = output;
		this.runtime = WhatsAppRuntime.start(settings, output);
	}

	public static ClientSession start(GameOutput output) {
		return new ClientSession(Settings.load(BridgePaths.configFile()), output);
	}

	public WhatsAppRuntime runtime() {
		return runtime;
	}

	public void tick(boolean keyPressed, boolean settingsOpen, Runnable openSettings) {
		runtime.tick();
		if (keyPressed) {
			runtime.requestOpenSettings();
		}
		if (runtime.consumeOpenSettings() && !settingsOpen) {
			openSettings.run();
		}
		if (!hinted && output.ready() && runtime.settings().full()) {
			hinted = true;
			output.showStatus("Full mode is on. Host this world with the mod, and chat is shared with the group.");
			return;
		}
		if (!hinted && output.ready() && !runtime.hasContact()) {
			hinted = true;
			output.showStatus(SettingsForm.NO_CONTACT_HINT);
		}
	}
}
