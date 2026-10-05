package dev.whatsappmc.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public final class Settings {
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	private static final SecureRandom RANDOM = new SecureRandom();

	public final Path file;
	public final String mode;
	public final String displayName;
	public final String allowedNumber;
	public final String prefix;
	public final String groupName;
	public final int port;
	public final String token;
	public final String problem;

	private Settings(Path file, String mode, String displayName, String allowedNumber, String prefix, String groupName, int port, String token, String problem) {
		this.file = file;
		this.mode = Modes.normalize(mode);
		this.displayName = displayName == null ? "" : displayName;
		this.allowedNumber = allowedNumber == null ? "" : allowedNumber;
		this.prefix = prefix == null ? Prefix.DEFAULT : prefix;
		this.groupName = groupName == null ? "" : groupName.trim();
		this.port = port;
		this.token = token == null ? "" : token;
		this.problem = problem;
	}

	public boolean full() {
		return Modes.full(mode);
	}

	public boolean canConnect() {
		return problem == null && token.length() >= 32 && port >= 1 && port <= 65535;
	}

	public boolean hasContact() {
		return canConnect() && PhoneNumbers.valid(allowedNumber);
	}

	public static Settings load(Path file) {
		if (!Files.isRegularFile(file)) {
			return missing(file, "Start the bridge once so it can create the local config.");
		}
		try {
			return fromJson(file, GSON.fromJson(Files.readString(file), JsonObject.class));
		} catch (IOException | JsonSyntaxException exception) {
			return missing(file, "The local config could not be read.");
		}
	}

	public static String validate(String displayName, String phoneRaw, String prefix) {
		if (!Prefix.valid(prefix)) {
			return "Prefix must be 2 to 16 characters and must not start with /.";
		}
		String digits = PhoneNumbers.digits(phoneRaw);
		if (!digits.isEmpty() && !PhoneNumbers.valid(digits)) {
			return "Use an international number, digits only, like 31600000000.";
		}
		if (displayName != null && displayName.trim().length() > 32) {
			return "Name must be 32 characters or less.";
		}
		return null;
	}

	public static String validateGroup(String groupName) {
		String group = groupName == null ? "" : groupName.trim();
		if (group.isEmpty() || group.length() > 64) {
			return "Group name must be 1 to 64 characters and must match the WhatsApp group exactly.";
		}
		if (group.indexOf('\n') >= 0 || group.indexOf('\r') >= 0) {
			return "Group name must be one line.";
		}
		return null;
	}

	public static Settings saveContact(Path file, String displayName, String phoneRaw, String prefix) throws IOException {
		Settings current = Files.isRegularFile(file) ? load(file) : null;
		String mode = current == null ? Modes.LITE : current.mode;
		String group = current == null ? "" : current.groupName;
		return saveSelection(file, mode, displayName, phoneRaw, prefix, group);
	}

	public static Settings saveSelection(Path file, String mode, String displayName, String phoneRaw, String prefix, String groupName) throws IOException {
		String normalized = Modes.normalize(mode);
		String error = Modes.full(normalized) ? validateGroup(groupName) : validate(displayName, phoneRaw, prefix);
		if (error != null) {
			throw new IllegalArgumentException(error);
		}
		Settings current = Files.isRegularFile(file) ? load(file) : null;
		String token = current != null && current.token.length() >= 32 ? current.token : newToken();
		int port = current != null && current.port >= 1 ? current.port : 17321;
		String digits = PhoneNumbers.digits(phoneRaw);
		String name = displayName == null ? "" : displayName.trim();
		if (name.isEmpty() && !digits.isEmpty()) {
			name = "WhatsApp";
		}
		String group = groupName == null ? "" : groupName.trim();
		JsonObject json = new JsonObject();
		json.addProperty("mode", normalized);
		json.addProperty("allowedNumber", digits);
		json.addProperty("displayName", name);
		json.addProperty("prefix", prefix == null ? Prefix.DEFAULT : prefix);
		json.addProperty("groupName", group);
		json.addProperty("port", port);
		json.addProperty("token", token);
		write(file, GSON.toJson(json));
		return load(file);
	}

	private static Settings missing(Path file, String problem) {
		return new Settings(file, Modes.LITE, "", "", Prefix.DEFAULT, "", 17321, "", problem);
	}

	private static Settings fromJson(Path file, JsonObject json) {
		if (json == null) {
			return missing(file, "The local config is empty.");
		}
		String mode = json.has("mode") ? Modes.normalize(json.get("mode").getAsString()) : Modes.LITE;
		String number = json.has("allowedNumber") ? PhoneNumbers.digits(json.get("allowedNumber").getAsString()) : "";
		String name = json.has("displayName") ? json.get("displayName").getAsString().trim() : "";
		String prefix = json.has("prefix") ? json.get("prefix").getAsString() : Prefix.DEFAULT;
		String group = json.has("groupName") ? json.get("groupName").getAsString().trim() : "";
		int port = json.has("port") ? json.get("port").getAsInt() : 17321;
		String token = json.has("token") ? json.get("token").getAsString() : "";
		if (!Prefix.valid(prefix)) {
			return new Settings(file, mode, name, number, Prefix.DEFAULT, group, port, token, "The reply prefix in the local config is invalid.");
		}
		if (token.length() < 32) {
			return new Settings(file, mode, name, number, prefix, group, port, token, "The local config has no link token. Start the bridge again.");
		}
		return new Settings(file, mode, name, number, prefix, group, port, token, null);
	}

	private static void write(Path file, String json) throws IOException {
		Files.createDirectories(file.getParent());
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(tmp, json + System.lineSeparator(), StandardCharsets.UTF_8);
		try {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static String newToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		StringBuilder out = new StringBuilder(bytes.length * 2);
		for (byte value : bytes) {
			out.append(Character.forDigit((value >> 4) & 0xF, 16));
			out.append(Character.forDigit(value & 0xF, 16));
		}
		return out.toString();
	}
}
