package dev.whatsappmc.core;

public final class Prefix {
	public static final String DEFAULT = ".h ";

	private Prefix() {
	}

	public static boolean valid(String prefix) {
		return prefix != null && prefix.length() >= 2 && prefix.length() <= 16 && !prefix.startsWith("/");
	}

	public static boolean shouldSwallow(String message, String prefix) {
		return valid(prefix) && message != null && message.startsWith(prefix);
	}

	public static String body(String message, String prefix) {
		return message.substring(prefix.length());
	}
}
