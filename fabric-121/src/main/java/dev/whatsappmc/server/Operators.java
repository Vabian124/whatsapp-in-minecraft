package dev.whatsappmc.server;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** 1.21.11 gates commands with Commands.hasPermission, not CommandSourceStack.hasPermission. */
public final class Operators {
	private Operators() {
	}

	public static boolean allowed(CommandSourceStack source) {
		return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
	}
}
