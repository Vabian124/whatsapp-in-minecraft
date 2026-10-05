package dev.whatsappmc.core;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public final class BridgeSocket implements Outbound {
	private final URI uri;
	private final String token;
	private final String lane;
	private final Consumer<Frame> frames;
	private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private final Listener listener = new Listener();
	private volatile WebSocket socket;
	private volatile boolean connecting;
	private volatile long nextAttemptAt;
	private volatile int backoffMs = 1000;

	public BridgeSocket(URI uri, String token, Consumer<Frame> frames) {
		this(uri, token, "direct", frames);
	}

	public BridgeSocket(URI uri, String token, String lane, Consumer<Frame> frames) {
		this.uri = uri;
		this.token = token;
		this.lane = lane == null || lane.isBlank() ? "direct" : lane;
		this.frames = frames;
	}

	public void close() {
		WebSocket current = socket;
		socket = null;
		if (current != null && !current.isOutputClosed()) {
			current.sendClose(WebSocket.NORMAL_CLOSURE, "closed");
		}
	}

	public void tick() {
		WebSocket current = socket;
		if (current != null && !current.isOutputClosed()) {
			return;
		}
		if (connecting) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now < nextAttemptAt) {
			return;
		}
		connecting = true;
		nextAttemptAt = now + backoffMs;
		backoffMs = Math.min(backoffMs * 2, 15000);
		client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5)).buildAsync(uri, listener).whenComplete((webSocket, error) -> {
			connecting = false;
			if (error != null) {
				socket = null;
				frames.accept(Frame.status("close"));
			}
		});
	}

	@Override
	public void send(String id, String text) {
		WebSocket current = socket;
		if (current == null || current.isOutputClosed()) {
			frames.accept(Frame.nack(id));
			return;
		}
		current.sendText(Frames.outgoing(id, text, lane), true);
	}

	private final class Listener implements WebSocket.Listener {
		private final StringBuilder incoming = new StringBuilder();

		@Override
		public void onOpen(WebSocket webSocket) {
			socket = webSocket;
			backoffMs = 1000;
			webSocket.sendText(Frames.auth(token), true);
			webSocket.request(1);
		}

		@Override
		public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			if (incoming.length() + data.length() > Frames.MAX_RAW) {
				incoming.setLength(0);
				webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "too large");
				return null;
			}
			incoming.append(data);
			if (last) {
				String raw = incoming.toString();
				incoming.setLength(0);
				Optional<Frame> frame = Frames.parse(raw);
				frame.ifPresent(frames);
			}
			webSocket.request(1);
			return null;
		}

		@Override
		public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
			socket = null;
			frames.accept(Frame.status("close"));
			return null;
		}

		@Override
		public void onError(WebSocket webSocket, Throwable error) {
			socket = null;
			frames.accept(Frame.status("close"));
		}
	}
}
