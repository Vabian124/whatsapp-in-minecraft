package dev.whatsappmc.core;

import java.net.URI;

/**
 * Lite is the default and the original goal: one private WhatsApp contact.
 * Full is optional and exclusive. The client closes its socket in Full so the
 * private thread is not also shown beside the shared world chat.
 */
public final class WhatsAppRuntime {
	private final ChatController chat;
	private volatile Settings current;
	private volatile BridgeSocket socket;
	private volatile boolean openSettings;
	private String linkState = "";
	private long lastLoad;

	private WhatsAppRuntime(Settings settings, GameOutput output) {
		this.current = settings;
		this.chat = new ChatController(settings.prefix, output, this::send);
	}

	public static WhatsAppRuntime start(Settings settings, GameOutput output) {
		return new WhatsAppRuntime(settings, output);
	}

	public void requestOpenSettings() {
		openSettings = true;
	}

	public boolean consumeOpenSettings() {
		if (!openSettings) {
			return false;
		}
		openSettings = false;
		return true;
	}

	public boolean allowChat(String message) {
		if (current.full()) {
			return true;
		}
		return chat.allowChat(message);
	}

	public void onCanceled(String message) {
		if (current.full()) {
			return;
		}
		if (!current.hasContact()) {
			note("Set a contact with /whatsapp.");
			return;
		}
		chat.onCanceled(message);
	}

	public void reload() {
		Settings loaded = Settings.load(current.file);
		current = loaded;
		chat.setPrefix(loaded.prefix);
		lastLoad = System.currentTimeMillis();
	}

	public Settings settings() {
		return current;
	}

	public boolean hasContact() {
		return current.hasContact();
	}

	public String connectionLabel() {
		Settings loaded = current;
		if (loaded.full()) {
			if (!loaded.canConnect()) {
				return loaded.problem == null ? "Bridge config is missing." : loaded.problem;
			}
			if (loaded.groupName.isBlank()) {
				return "Full mode. Enter the WhatsApp group name, then press Save.";
			}
			return "Full mode. This world shares chat with " + loaded.groupName + ".";
		}
		if (!loaded.canConnect()) {
			return loaded.problem == null ? "Bridge config is missing." : loaded.problem;
		}
		return switch (linkState) {
			case "open" -> loaded.hasContact()
				? "Connected. In chat, type " + loaded.prefix + "and your message."
				: "Connected. Enter a phone number, then press Save.";
			case "qr" -> "Scan the QR code in the bridge window. WhatsApp, Linked devices.";
			case "close" -> "The bridge is not connected. Leave that window open.";
			default -> "Looking for the bridge on this computer.";
		};
	}

	public void tick() {
		long now = System.currentTimeMillis();
		if (now - lastLoad > 2000) {
			lastLoad = now;
			Settings loaded = Settings.load(current.file);
			if (!loaded.prefix.equals(current.prefix) || !loaded.allowedNumber.equals(current.allowedNumber) || !loaded.token.equals(current.token) || !loaded.mode.equals(current.mode) || !loaded.groupName.equals(current.groupName)) {
				current = loaded;
				chat.setPrefix(loaded.prefix);
			}
		}
		ensureSocket();
		if (current.full()) {
			BridgeSocket active = socket;
			socket = null;
			if (active != null) {
				active.close();
			}
			return;
		}
		BridgeSocket active = socket;
		if (active != null) {
			active.tick();
		}
		chat.tick();
	}

	private void send(String id, String text) {
		BridgeSocket active = socket;
		if (active == null) {
			onFrame(Frame.nack(id));
			return;
		}
		active.send(id, text);
	}

	private void ensureSocket() {
		Settings loaded = current;
		if (!loaded.canConnect() || loaded.full() || socket != null) {
			return;
		}
		socket = new BridgeSocket(URI.create("ws://127.0.0.1:" + loaded.port), loaded.token, this::onFrame);
	}

	private void onFrame(Frame frame) {
		if ("status".equals(frame.type) && frame.state != null) {
			linkState = frame.state;
		}
		chat.onFrame(frame);
	}

	private void note(String text) {
		Frame frame = new Frame();
		frame.type = "note";
		frame.text = text;
		chat.onFrame(frame);
	}
}
