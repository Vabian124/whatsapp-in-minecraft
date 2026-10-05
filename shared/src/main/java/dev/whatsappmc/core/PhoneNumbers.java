package dev.whatsappmc.core;

public final class PhoneNumbers {
	private PhoneNumbers() {
	}

	public static String digits(String raw) {
		if (raw == null || raw.isEmpty()) {
			return "";
		}
		StringBuilder out = new StringBuilder(raw.length());
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (c >= '0' && c <= '9') {
				out.append(c);
			}
		}
		if (out.length() > 2 && out.charAt(0) == '0' && out.charAt(1) == '0') {
			out.delete(0, 2);
		}
		return out.toString();
	}

	public static boolean valid(String digits) {
		return digits != null && digits.length() >= 8 && digits.length() <= 15;
	}
}
