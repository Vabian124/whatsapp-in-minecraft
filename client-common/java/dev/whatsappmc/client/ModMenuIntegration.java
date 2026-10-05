package dev.whatsappmc.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.whatsappmc.core.WhatsAppRuntime;

public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> {
			WhatsAppRuntime runtime = WhatsAppClient.runtime();
			if (runtime == null) {
				return parent;
			}
			return new SettingsScreen(runtime, parent);
		};
	}
}
