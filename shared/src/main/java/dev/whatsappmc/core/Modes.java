package dev.whatsappmc.core;

public final class Modes {
	public static final String LITE = "lite";
	public static final String FULL = "full";

	private Modes() {
	}

	public static boolean full(String mode) {
		return FULL.equals(mode);
	}

	public static String normalize(String mode) {
		return full(mode) ? FULL : LITE;
	}
}
