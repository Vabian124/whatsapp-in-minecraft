package dev.whatsappmc.core;

import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public final class Frames {
	public static final int MAX_RAW = 8192;
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

	private Frames() {
	}

	public static String auth(String token) {
		Frame frame = new Frame();
		frame.type = "auth";
		frame.token = token;
		return GSON.toJson(frame);
	}

	public static String outgoing(String id, String text) {
		return outgoing(id, text, "direct");
	}

	public static String outgoing(String id, String text, String lane) {
		Frame frame = new Frame();
		frame.type = "out";
		frame.id = id;
		frame.text = text;
		frame.lane = lane;
		return GSON.toJson(frame);
	}

	public static Optional<Frame> parse(String raw) {
		if (raw == null || raw.length() > MAX_RAW) {
			return Optional.empty();
		}
		try {
			Frame frame = GSON.fromJson(raw, Frame.class);
			if (frame == null || frame.v != 1 || frame.type == null || frame.type.isBlank()) {
				return Optional.empty();
			}
			if (frame.text != null && frame.text.length() > Sanitizer.MAX_TEXT + 200) {
				return Optional.empty();
			}
			if (frame.id != null && frame.id.length() > 64) {
				return Optional.empty();
			}
			return Optional.of(frame);
		} catch (JsonSyntaxException ignored) {
			return Optional.empty();
		}
	}
}
