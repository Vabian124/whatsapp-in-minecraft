package dev.whatsappmc.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class TokenCheck {
	private TokenCheck() {
	}

	public static boolean equals(String left, String right) {
		byte[] a = left == null ? new byte[0] : left.getBytes(StandardCharsets.UTF_8);
		byte[] b = right == null ? new byte[0] : right.getBytes(StandardCharsets.UTF_8);
		if (a.length != b.length) {
			MessageDigest.isEqual(a, a);
			return false;
		}
		return MessageDigest.isEqual(a, b);
	}
}
