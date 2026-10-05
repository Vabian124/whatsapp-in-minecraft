package dev.whatsappmc.server;

import dev.whatsappmc.core.GameOutput;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/** Draws Full-mode lines into the world chat. The text is not written to the logger. */
final class ServerOutput implements GameOutput {
	private final MinecraftServer server;

	ServerOutput(MinecraftServer server) {
		this.server = server;
	}

	@Override
	public void showIncoming(String name, String body) {
		broadcast(Component.literal("[" + name + "] ").withStyle(ChatFormatting.GREEN).append(Component.literal(body)));
	}

	@Override
	public void showOwn(String body) {
		broadcast(Component.literal("[You] ").withStyle(ChatFormatting.GRAY).append(Component.literal(body)));
	}

	@Override
	public void showStatus(String text) {
		broadcast(Component.literal(text).withStyle(ChatFormatting.YELLOW));
	}

	@Override
	public boolean ready() {
		return server.getPlayerList() != null;
	}

	private void broadcast(Component line) {
		if (server.getPlayerList() != null) {
			server.getPlayerList().broadcastSystemMessage(line, false);
		}
	}
}
