package dev.whatsappmc.server;

import java.net.URI;
import java.util.stream.Collectors;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.whatsappmc.core.BridgePaths;
import dev.whatsappmc.core.BridgeSocket;
import dev.whatsappmc.core.GroupRelay;
import dev.whatsappmc.core.Modes;
import dev.whatsappmc.core.Settings;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Full mode only. Lite never starts this link, so a private chat stays on the
 * player client and is not copied into the dedicated-server log.
 * Player chat is mirrored. System messages are not, or a WhatsApp line we
 * broadcast would be sent straight back.
 */
public final class WhatsAppServer implements ModInitializer {
	private Settings settings = Settings.load(BridgePaths.configFile());
	private long lastLoad;
	private GroupRelay relay;
	private BridgeSocket socket;

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
			Commands.literal("whatsapp")
				.requires(Operators::allowed)
				.then(Commands.literal("lite").executes(ctx -> switchMode(ctx.getSource(), Modes.LITE)))
				.then(Commands.literal("full").executes(ctx -> switchMode(ctx.getSource(), Modes.FULL)))
				.then(Commands.literal("group").then(Commands.argument("name", StringArgumentType.greedyString())
					.executes(ctx -> setGroup(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
		));
		ServerMessageEvents.CHAT_MESSAGE.register(this::onChat);
		ServerTickEvents.END_SERVER_TICK.register(this::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> stop());
	}

	private void onChat(PlayerChatMessage message, ServerPlayer sender, net.minecraft.network.chat.ChatType.Bound bound) {
		if (!settings.full() || relay == null || sender == null) {
			return;
		}
		relay.fromPlayer(sender.getName().getString(), message.signedContent());
	}

	private void tick(MinecraftServer server) {
		long now = System.currentTimeMillis();
		if (now - lastLoad > 2000) {
			lastLoad = now;
			settings = Settings.load(BridgePaths.configFile());
		}
		boolean link = settings.full() && settings.canConnect() && !settings.groupName.isBlank();
		if (!link) {
			stop();
			return;
		}
		if (relay == null) {
			start(server);
		}
		if (socket != null) {
			socket.tick();
		}
		if (relay != null) {
			relay.tick();
			if (relay.consumePlayers()) {
				String names = server.getPlayerList().getPlayers().stream()
					.map(player -> player.getName().getString())
					.collect(Collectors.joining(", "));
				relay.sendRoster(names.isEmpty() ? "Nobody is online." : "Online: " + names);
			}
		}
	}

	private void start(MinecraftServer server) {
		ServerOutput output = new ServerOutput(server);
		relay = new GroupRelay(output, (id, text) -> {
			BridgeSocket active = socket;
			if (active != null) {
				active.send(id, text);
			}
		});
		socket = new BridgeSocket(URI.create("ws://127.0.0.1:" + settings.port), settings.token, "group", relay::onFrame);
	}

	private void stop() {
		if (socket != null) {
			socket.close();
		}
		socket = null;
		relay = null;
	}

	private int switchMode(CommandSourceStack source, String mode) {
		try {
			Settings current = Settings.load(settings.file);
			settings = Settings.saveSelection(current.file, mode, current.displayName, current.allowedNumber, current.prefix, current.groupName);
			tell(source, Modes.full(mode)
				? "Full mode. Share this world's chat with /whatsapp group <exact group name>."
				: "Lite mode. One private chat, typed with your prefix.");
			return 1;
		} catch (Exception exception) {
			source.sendFailure(Component.literal(exception.getMessage() == null ? "Could not save the mode." : exception.getMessage()));
			return 0;
		}
	}

	private int setGroup(CommandSourceStack source, String name) {
		String error = Settings.validateGroup(name);
		if (error != null) {
			source.sendFailure(Component.literal(error));
			return 0;
		}
		try {
			Settings current = Settings.load(settings.file);
			settings = Settings.saveSelection(current.file, current.mode, current.displayName, current.allowedNumber, current.prefix, name);
			tell(source, "Group set to " + settings.groupName + ". Full mode uses it. Lite ignores it.");
			return 1;
		} catch (Exception exception) {
			source.sendFailure(Component.literal("Could not save the group."));
			return 0;
		}
	}

	private static void tell(CommandSourceStack source, String text) {
		source.sendSuccess(() -> Component.literal(text), false);
	}
}
