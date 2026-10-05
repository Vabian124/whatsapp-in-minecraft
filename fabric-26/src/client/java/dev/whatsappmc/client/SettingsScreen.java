package dev.whatsappmc.client;

import dev.whatsappmc.core.Modes;
import dev.whatsappmc.core.Settings;
import dev.whatsappmc.core.SettingsForm;
import dev.whatsappmc.core.WhatsAppRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class SettingsScreen extends Screen {
	private final WhatsAppRuntime runtime;
	private final Screen parent;
	private EditBox nameBox;
	private EditBox phoneBox;
	private EditBox prefixBox;
	private EditBox groupBox;
	private String mode;
	private String notice = "";

	SettingsScreen(WhatsAppRuntime runtime, Screen parent) {
		super(Component.literal(SettingsForm.TITLE));
		this.runtime = runtime;
		this.parent = parent;
		this.mode = runtime.settings().mode;
	}

	@Override
	public void onClose() {
		if (parent != null) {
			this.minecraft.setScreenAndShow(parent);
		} else {
			super.onClose();
		}
	}

	@Override
	protected void init() {
		Settings settings = runtime.settings();
		SettingsForm.Placement place = SettingsForm.Placement.of(this.width, this.height);
		nameBox = null;
		phoneBox = null;
		prefixBox = null;
		groupBox = null;
		addRenderableWidget(Button.builder(Component.literal(Modes.LITE.equals(mode) ? "[Lite]" : "Lite"), button -> choose(Modes.LITE))
			.bounds(place.left(), place.toggleY(), SettingsForm.BUTTON_WIDTH, SettingsForm.FIELD_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.literal(Modes.full(mode) ? "[Full]" : "Full"), button -> choose(Modes.FULL))
			.bounds(place.doneX(), place.toggleY(), SettingsForm.BUTTON_WIDTH, SettingsForm.FIELD_HEIGHT).build());
		if (Modes.full(mode)) {
			groupBox = box(place.left(), place.fieldY(0), "Group", settings.groupName, 64);
		} else {
			nameBox = box(place.left(), place.fieldY(0), "Name", settings.displayName, 32);
			phoneBox = box(place.left(), place.fieldY(1), "Phone", settings.allowedNumber, 18);
			prefixBox = box(place.left(), place.fieldY(2), "Prefix", settings.prefix, 16);
		}
		addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
			.bounds(place.left(), place.buttonY(), SettingsForm.BUTTON_WIDTH, SettingsForm.FIELD_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
			.bounds(place.doneX(), place.buttonY(), SettingsForm.BUTTON_WIDTH, SettingsForm.FIELD_HEIGHT).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		SettingsForm.Placement place = SettingsForm.Placement.of(this.width, this.height);
		graphics.centeredText(this.font, this.title, this.width / 2, place.titleY(), SettingsForm.TITLE_COLOR);
		if (Modes.full(mode)) {
			label(graphics, SettingsForm.GROUP_LABEL, place.left(), place.labelY(0));
			label(graphics, SettingsForm.FULL_HINT, place.left(), place.labelY(1));
		} else {
			label(graphics, SettingsForm.NAME_LABEL, place.left(), place.labelY(0));
			label(graphics, SettingsForm.PHONE_LABEL, place.left(), place.labelY(1));
			label(graphics, SettingsForm.PREFIX_LABEL, place.left(), place.labelY(2));
		}
		int statusY = place.statusY();
		for (var line : this.font.split(Component.literal(runtime.connectionLabel()), SettingsForm.FIELD_WIDTH)) {
			graphics.text(this.font, line, place.left(), statusY, SettingsForm.STATUS_COLOR);
			statusY += 10;
		}
		if (!notice.isEmpty()) {
			graphics.text(this.font, Component.literal(notice), place.left(), statusY + 4, SettingsForm.NOTICE_COLOR);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private EditBox box(int x, int y, String label, String value, int max) {
		EditBox box = new EditBox(this.font, x, y, SettingsForm.FIELD_WIDTH, SettingsForm.FIELD_HEIGHT, Component.literal(label));
		box.setMaxLength(max);
		box.setValue(value == null ? "" : value);
		addRenderableWidget(box);
		return box;
	}

	private void label(GuiGraphicsExtractor graphics, String text, int x, int y) {
		graphics.text(this.font, Component.literal(text), x, y, SettingsForm.LABEL_COLOR);
	}

	private void choose(String next) {
		mode = next;
		clearWidgets();
		init();
	}

	private void save() {
		Settings current = runtime.settings();
		SettingsForm.SaveResult result = SettingsForm.save(
			current.file,
			mode,
			nameBox == null ? current.displayName : nameBox.getValue(),
			phoneBox == null ? current.allowedNumber : phoneBox.getValue(),
			prefixBox == null ? current.prefix : prefixBox.getValue(),
			groupBox == null ? current.groupName : groupBox.getValue()
		);
		notice = result.notice();
		if (result.saved()) {
			runtime.reload();
		}
	}
}
