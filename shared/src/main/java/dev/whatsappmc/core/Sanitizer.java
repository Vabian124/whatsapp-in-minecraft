package dev.whatsappmc.core;

import java.util.ArrayList;
import java.util.List;

public final class Sanitizer {
	public static final int MAX_TEXT = 4000;
	public static final int CHUNK = 240;

	private Sanitizer() {
	}

	public static String sanitize(String raw, int max) {
		if (raw == null || raw.isEmpty() || max <= 0) {
			return "";
		}
		StringBuilder out = new StringBuilder(Math.min(raw.length(), max));
		boolean pendingSpace = false;
		for (int i = 0; i < raw.length(); ) {
			int cp = raw.codePointAt(i);
			i += Character.charCount(cp);
			if (cp == 0x00A7 || cp == 0x7F || (cp < 0x20 && cp != '\n' && cp != '\r' && cp != '\t')) {
				continue;
			}
			if (cp == '\n' || cp == '\r' || cp == '\t') {
				pendingSpace = out.length() > 0;
				continue;
			}
			if (pendingSpace) {
				if (out.length() >= max) {
					break;
				}
				out.append(' ');
				pendingSpace = false;
			}
			if (out.length() >= max) {
				break;
			}
			out.appendCodePoint(cp);
		}
		return trim(out);
	}

	public static List<String> chunks(String text, int max) {
		List<String> parts = new ArrayList<>();
		if (text == null || text.isEmpty() || max <= 0) {
			return parts;
		}
		int index = 0;
		while (index < text.length()) {
			int end = Math.min(text.length(), index + max);
			if (end < text.length() && Character.isLowSurrogate(text.charAt(end))) {
				end--;
			}
			if (end <= index) {
				end = Math.min(text.length(), index + 1);
			}
			parts.add(text.substring(index, end));
			index = end;
		}
		return parts;
	}

	private static String trim(StringBuilder out) {
		int start = 0;
		int end = out.length();
		while (start < end && out.charAt(start) == ' ') {
			start++;
		}
		while (end > start && out.charAt(end - 1) == ' ') {
			end--;
		}
		return out.substring(start, end);
	}
}
