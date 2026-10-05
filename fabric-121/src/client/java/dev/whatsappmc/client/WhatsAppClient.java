package dev.whatsappmc.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.whatsappmc.core.ClientSession;
import dev.whatsappmc.core.WhatsAppRuntime;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class WhatsAppClient implements ClientModInitializer {
	private static ClientSession session;
	private KeyMapping openKey;

	public static WhatsAppRuntime runtime() {
		return session == null ? null : session.runtime();
	}

	@Override
	public void onInitializeClient() {
		session = ClientSession.start(new McGame());
		WhatsAppRuntime runtime = session.runtime();
		openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.whatsappchat.open",
			InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(),
			KeyMapping.Category.MISC
		));
		ClientSendMessageEvents.ALLOW_CHAT.register(runtime::allowChat);
		ClientSendMessageEvents.CHAT_CANCELED.register(runtime::onCanceled);
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
			ClientCommandManager.literal("whatsapp")
				.executes(context -> {
					runtime.requestOpenSettings();
					return 1;
				})
		));
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void tick(Minecraft minecraft) {
		boolean clicked = false;
		while (openKey.consumeClick()) {
			clicked = true;
		}
		session.tick(clicked, minecraft.screen instanceof SettingsScreen, () ->
			minecraft.setScreen(new SettingsScreen(session.runtime(), null))
		);
	}
}
