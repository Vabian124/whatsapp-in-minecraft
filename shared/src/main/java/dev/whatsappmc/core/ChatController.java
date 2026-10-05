package dev.whatsappmc.core;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;

public final class ChatController {
	private final GameOutput output;
	private final Outbound outbound;
	private final Object gate = new Object();
	private final ArrayDeque<Runnable> jobs = new ArrayDeque<>();
	private final LinkedHashMap<String, String> pending = new LinkedHashMap<>();
	private String prefix;
	private String shownStatus = "";
	private int nextId = 1;
	private boolean skipped;

	public ChatController(String prefix, GameOutput output, Outbound outbound) {
		this.prefix = prefix;
		this.output = output;
		this.outbound = outbound;
	}

	public void setPrefix(String prefix) {
		synchronized (gate) {
			this.prefix = prefix;
		}
	}

	public boolean allowChat(String message) {
		String current;
		synchronized (gate) {
			current = prefix;
		}
		return !Prefix.shouldSwallow(message, current);
	}

	public void onCanceled(String message) {
		String current;
		synchronized (gate) {
			current = prefix;
		}
		if (!Prefix.shouldSwallow(message, current)) {
			return;
		}
		String body = Sanitizer.sanitize(Prefix.body(message, current), Sanitizer.MAX_TEXT);
		if (body.isEmpty()) {
			enqueueStatus("Nothing to send.");
			return;
		}
		String id;
		synchronized (gate) {
			id = "c" + nextId++;
			pending.put(id, body);
			while (pending.size() > 20) {
				var oldest = pending.keySet().iterator().next();
				pending.remove(oldest);
			}
		}
		outbound.send(id, body);
	}

	public void onFrame(Frame frame) {
		if (frame == null || frame.type == null || "group".equals(frame.lane)) {
			return;
		}
		switch (frame.type) {
			case "in" -> showIncoming(frame);
			case "ack" -> showAck(frame.id);
			case "nack" -> {
				forget(frame.id);
				enqueueStatus("Message was not delivered.");
			}
			case "status" -> enqueueStatus(statusText(frame.state));
			case "note" -> enqueueStatus(Sanitizer.sanitize(frame.text, 200));
			default -> {
			}
		}
	}

	public void tick() {
		int shown = 0;
		while (shown < 5) {
			if (!output.ready()) {
				return;
			}
			Runnable job;
			boolean tellSkipped = false;
			synchronized (gate) {
				job = jobs.pollFirst();
				if (job == null && skipped) {
					skipped = false;
					tellSkipped = true;
				}
			}
			if (tellSkipped) {
				output.showStatus("Some WhatsApp lines were skipped.");
				shown++;
				continue;
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
		for (String part : Sanitizer.chunks(body, Sanitizer.CHUNK)) {
			String lineName = name;
			String line = part;
			enqueue(() -> output.showIncoming(lineName, line));
		}
	}

	private void showAck(String id) {
		String body = forget(id);
		if (body != null) {
			enqueue(() -> output.showOwn(body));
		}
	}

	private String forget(String id) {
		if (id == null) {
			return null;
		}
		synchronized (gate) {
			return pending.remove(id);
		}
	}

	private static String statusText(String state) {
		if (state == null) {
			return null;
		}
		return switch (state) {
			case "open" -> "WhatsApp connected.";
			case "qr" -> "Scan the QR code in the bridge window.";
			case "close" -> "WhatsApp disconnected.";
			default -> null;
		};
	}

	private void enqueueStatus(String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		enqueue(() -> {
			if (text.equals(shownStatus)) {
				return;
			}
			shownStatus = text;
			output.showStatus(text);
		});
	}

	private void enqueue(Runnable job) {
		synchronized (gate) {
			if (jobs.size() >= 50) {
				jobs.removeFirst();
				skipped = true;
			}
			jobs.addLast(job);
		}
	}
}
