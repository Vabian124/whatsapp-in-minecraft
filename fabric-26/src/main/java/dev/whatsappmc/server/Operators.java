package dev.whatsappmc.server;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;

/** 26.3 replaced the old numeric permission level with PermissionSet. */
public final class Operators {
	private Operators() {
	}

	public static boolean allowed(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}
}
