package dev.whatsappmc.core;

import java.io.IOException;
import java.nio.file.Path;

public final class SettingsForm {
	public static final String TITLE = "WhatsappMC";
	public static final String NO_CONTACT_HINT = "Open Esc, Mods, WhatsappMC. Or type /whatsapp.";
	public static final String NAME_LABEL = "Name shown in your chat";
	public static final String PHONE_LABEL = "Their international number";
	public static final String PREFIX_LABEL = "Reply prefix. Default is .h ";
	public static final String GROUP_LABEL = "WhatsApp group name, exact";
	public static final String FULL_HINT = "Full mode shares this world's chat with that group. Lite keeps one private chat.";
	public static final String SAVED = "Saved.";
	public static final String SAVE_FAILED = "Could not save the config.";

	public static final int FIELD_WIDTH = 220;
	public static final int FIELD_HEIGHT = 20;
	public static final int BUTTON_WIDTH = 100;
	public static final int BUTTON_GAP = 20;
	public static final int ROW = 36;
	public static final int TITLE_COLOR = 0xFFFFFF;
	public static final int LABEL_COLOR = 0xFFAAAAAA;
	public static final int STATUS_COLOR = 0xFFDDDDDD;
	public static final int NOTICE_COLOR = 0xFFFFCC66;

	private SettingsForm() {
	}

	public record Placement(int left, int fieldsY, int labelsY) {
		public static Placement of(int width, int height) {
			return new Placement(width / 2 - 110, height / 2 - 46, height / 2 - 58);
		}

		public int fieldY(int row) {
			return fieldsY + row * ROW;
		}

		public int labelY(int row) {
			return labelsY + row * ROW;
		}

		public int buttonY() {
			return fieldsY + 108;
		}

		public int titleY() {
			return labelsY - 28;
		}

		public int toggleY() {
			return titleY() + 14;
		}

		public int statusY() {
			return labelsY + 136;
		}

		public int doneX() {
			return left + BUTTON_WIDTH + BUTTON_GAP;
		}
	}

	public record SaveResult(boolean saved, String notice) {
	}

	public static SaveResult save(Path file, String mode, String displayName, String phoneRaw, String prefix, String groupName) {
		String error = Modes.full(mode) ? Settings.validateGroup(groupName) : Settings.validate(displayName, phoneRaw, prefix);
		if (error != null) {
			return new SaveResult(false, error);
		}
		try {
			Settings.saveSelection(file, mode, displayName, phoneRaw, prefix, groupName);
			return new SaveResult(true, SAVED);
		} catch (IOException exception) {
			return new SaveResult(false, SAVE_FAILED);
		}
	}
}
