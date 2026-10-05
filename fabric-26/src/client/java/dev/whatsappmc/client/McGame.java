package dev.whatsappmc.client;

import dev.whatsappmc.core.GameOutput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

final class McGame implements GameOutput {
	@Override
	public void showIncoming(String name, String body) {
		add(Component.literal("[" + name + "] ").withStyle(ChatFormatting.GREEN).append(Component.literal(body)), true);
	}

	@Override
	public void showOwn(String body) {
		add(Component.literal("[You] ").withStyle(ChatFormatting.GRAY).append(Component.literal(body)), false);
	}

	@Override
	public void showStatus(String text) {
		add(Component.literal(text).withStyle(ChatFormatting.YELLOW), false);
	}

	@Override
	public boolean ready() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null && minecraft.gui != null && minecraft.gui.hud != null;
	}

	private void add(Component line, boolean sound) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.gui == null || minecraft.gui.hud == null) {
			return;
		}
		minecraft.gui.hud.getChat().addClientSystemMessage(line);
		if (sound) {
			minecraft.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.45F, 1.6F);
		}
	}
}
