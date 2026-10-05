package dev.whatsappmc.core;

import java.util.ArrayDeque;

/** Full-mode queue. Direct-lane frames are ignored so a private chat cannot appear in the world. */
public final class GroupRelay {
	private final GameOutput output;
	private final Outbound outbound;
	private final Object gate = new Object();
	private final ArrayDeque<Runnable> jobs = new ArrayDeque<>();
	private boolean askPlayers;
	private int nextId = 1;

	public GroupRelay(GameOutput output, Outbound outbound) {
		this.output = output;
		this.outbound = outbound;
	}

	public void onFrame(Frame frame) {
		if (frame == null || frame.type == null) {
			return;
		}
		if ("ask".equals(frame.type) && "players".equals(frame.text)) {
			synchronized (gate) {
				askPlayers = true;
			}
			return;
		}
		if (!"group".equals(frame.lane) && !"status".equals(frame.type)) {
			return;
		}
		switch (frame.type) {
			case "in" -> showIncoming(frame);
			case "status" -> {
				String text = statusText(frame.state);
				if (text != null) {
					enqueue(() -> output.showStatus(text));
				}
			}
			case "nack" -> enqueue(() -> output.showStatus("Message was not delivered."));
			default -> {
			}
		}
	}

	public boolean consumePlayers() {
		synchronized (gate) {
			if (!askPlayers) {
				return false;
			}
			askPlayers = false;
			return true;
		}
	}

	public void fromPlayer(String player, String text) {
		String name = Sanitizer.sanitize(player, 32);
		String body = Sanitizer.sanitize(text, Sanitizer.MAX_TEXT);
		if (name.isEmpty() || body.isEmpty()) {
			return;
		}
		send("g" + nextId(), name + ": " + body);
	}

	public void sendRoster(String text) {
		String body = Sanitizer.sanitize(text, Sanitizer.MAX_TEXT);
		if (!body.isEmpty()) {
			send("g" + nextId(), body);
		}
	}

	public void tick() {
		int shown = 0;
		while (shown < 5) {
			if (!output.ready()) {
				return;
			}
			Runnable job;
			synchronized (gate) {
				job = jobs.pollFirst();
			}
			if (job == null) {
				return;
			}
			job.run();
			shown++;
		}
	}

	private void showIncoming(Frame frame) {
		String body = Sanitizer.sanitize(frame.text, Sanitizer.MAX_TEXT);
		if (body.isEmpty()) {
			return;
		}
		String name = Sanitizer.sanitize(frame.name == null || frame.name.isBlank() ? "WhatsApp" : frame.name, 32);
		if (name.isEmpty()) {
			name = "WhatsApp";
		}
		String lineName = name;
		enqueue(() -> output.showIncoming(lineName, body));
	}

	private void send(String id, String text) {
		outbound.send(id, text);
	}

	private int nextId() {
		synchronized (gate) {
			return nextId++;
		}
	}

	private static String statusText(String state) {
		if (state == null) {
			return null;
		}
		return switch (state) {
			case "open" -> "WhatsApp group connected.";
			case "qr" -> "Scan the QR code in the bridge window.";
			case "close" -> "WhatsApp group disconnected.";
			default -> null;
		};
	}

	private void enqueue(Runnable job) {
		synchronized (gate) {
			if (jobs.size() >= 50) {
				jobs.removeFirst();
			}
			jobs.addLast(job);
		}
	}
}
