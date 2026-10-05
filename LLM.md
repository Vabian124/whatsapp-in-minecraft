# LLM handoff

Read this before changing the mod. The player-facing setup is `README.md`. This file is the map of decisions, traps, and unfinished work.

## Product

The main goal is Lite mode: one person plays Minecraft and talks to one WhatsApp contact. That contact installs nothing. Replies use a prefix (default `.h `, including the trailing space) so normal Minecraft chat still goes to the server. The private thread must not be sent to a dedicated server and must not be written to a log.

Full mode is the other position of the same switch. It is closer to [WALink](https://github.com/davidsaltacc/walink): a world the player hosts shares ordinary player chat with one WhatsApp group. It is optional. Do not make Full the default, and do not run both links at once. `WhatsAppRuntime` closes the client socket while Full is on so the private chat is not also painted next to the shared world chat.

There is no personal WhatsApp API. The bridge is a linked device through Baileys. Keep the terms warning in the README. Do not add bulk messaging, unknown recipients, or a way to scrape chats.

## Modes

Config field `mode` is `lite` or `full`. Missing or unknown values are Lite. `groupName` is the exact WhatsApp group subject. Both live in the local config, not in the repo.

- Lite client: swallows only lines that start with the prefix, sends them on lane `direct`, shows lane `direct` messages.
- Lite server: `WhatsAppServer` stays idle. It still registers `/whatsapp` for operators.
- Full client: does not swallow chat and does not keep a socket.
- Full server: player chat (`ServerMessageEvents.CHAT_MESSAGE` only) goes out on lane `group`. Incoming lane `group` is broadcast with `broadcastSystemMessage`. System messages are not hooked. Hooking them would echo every WhatsApp line back into the group.

Operators: `/whatsapp lite`, `/whatsapp full`, `/whatsapp group <exact name>`. The screen buttons write the same file.

## Layout

- `shared` — Java with no Minecraft imports. Protocol, settings, `ChatController` (Lite), `GroupRelay` (Full). Compiled into each mod through the main source set.
- `client-common` — Mod Menu entry only. Client source set.
- `server-common` — `WhatsAppServer` and `ServerOutput`. Main source set.
- `fabric-26` — Minecraft 26.3. Unobfuscated Mojmap. Java 25. Loom plugin `net.fabricmc.fabric-loom` (no Yarn remap).
- `fabric-121` — Minecraft 1.21.11. Java 21. Plugin `net.fabricmc.fabric-loom-remap` and `loom.officialMojangMappings()`.
- `bridge` — Node 22+ TypeScript. Baileys session and the loopback WebSocket.

One jar cannot span Java 25 and Java 21. Keep two Fabric projects. Shared code must stay compatible with Java 21.

`fabric.mod.json` `environment` is `*` so a dedicated server can load Full mode. Client entrypoints still run only on the client. The `modmenu` key is loaded by Mod Menu, which is client-only, so a server without Mod Menu does not instantiate that class.

## Build and test

From the repo root:

```bat
gradlew.bat buildAll
```

That runs shared JUnit, `npm run check` in `bridge` (eslint plus tests), and both jars. The bridge tests include a dry socket on `127.0.0.1` with an ephemeral port. They must not start Baileys or print a QR code. `bridge/src/index.ts` only calls `main()` when it is the process entry, because `node --test` must not link WhatsApp.

Do not point `node --test` at the whole `dist` folder. Test files are named explicitly in `package.json`.

## Protocol

WebSocket on `127.0.0.1` only. Never bind `localhost` (that can resolve to something else). First frame is `auth` with the shared token, 2 second timeout, constant-time compare. Frames are version `v: 1`, max 8192 bytes.

Lanes:

- `direct` — the one phone number. Reply to the inbound `remoteJid`, which is often `@lid`. The phone may be in `remoteJidAlt` or `senderPn`. Persist paired lids in `lids.json`.
- `group` — `@g.us` only. The first message whose group subject equals `groupName` exactly is bound and stored in `group.json`. Later messages from that jid are accepted even if the subject lookup is skipped. A different group with the same name is not accepted after the bind. Changing `groupName` clears the bind.

Outgoing queue is 20 messages, about one send per second. Text cap is 4000 (`limits.ts` and `Sanitizer.MAX_TEXT`). Do not download media. Placeholders: `[photo]`, `[video]`, `[voice]`, `[sticker]`, `[file]`, `[contact]`, `[location]`.

Ignore groups in Lite, broadcasts, newsletters, history from before process start, `fromMe`, and decrypt failures. `.mc players` becomes an `ask` frame the server answers with a roster. `.mc help` is answered by the bridge. Do not log message text, tokens, or session keys.

## Version traps

Checked against the jars this repo already compiled. Re-check with `javap` before copying an API across versions.

26.3 client:

- Chat HUD is `minecraft.gui.hud.getChat().addClientSystemMessage(Component)`. There is no `gui.getChat().addMessage`.
- Screen draw is `extractRenderState(GuiGraphicsExtractor, ...)`, not `render(GuiGraphics)`.
- Open a screen with `setScreenAndShow`. Current screen is `minecraft.gui.screen()`.
- Key helper is `KeyMappingHelper`, input type `InputConstants.Type.KEYBOARD`.
- Client commands use `ClientCommands.literal`. `FabricClientCommandSource.attended()` exists. Keep it on 26.3.
- Operator commands use `source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)`. There is no `hasPermission(int)`.
- Sound is `SoundEvents.NOTE_BLOCK_PLING.value()` because the field is a Holder.
- Mod Menu 21.0.0, `compileOnly`, from `https://maven.terraformersmc.com/releases/`.

1.21.11 client:

- Chat HUD is `minecraft.gui.getChat().addMessage`.
- Screen draw is `render(GuiGraphics)`. Open with `setScreen`. Current screen is `minecraft.screen`.
- Key helper is `KeyBindingHelper`, input type `InputConstants.Type.KEYSYM`.
- Client commands use `ClientCommandManager.literal`. Do not call `attended()`. It does not exist and fails the compile.
- Operator commands use `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`. `CommandSourceStack.hasPermission(int)` does not exist on 1.21.11.
- Mod Menu 17.0.1 as `modCompileOnly` so Loom remaps it. 16.0.0 is the wrong version range.

Both:

- `ServerMessageEvents.CHAT_MESSAGE` is `(PlayerChatMessage, ServerPlayer, ChatType.Bound)`.
- `CommandRegistrationCallback` is `(dispatcher, CommandBuildContext, Commands.CommandSelection)`.
- `PlayerChatMessage.signedContent()` and `broadcastSystemMessage(Component, boolean)` exist on both.
- Gson coordinate is `com.google.code.gson:gson`. The shared module declares it. The Fabric projects must not. Minecraft already provides `com.google.gson`, and the wrong Maven group 404s under Loom.
- Do not put Henna's number, or any real number, in tracked files. Tests use `31600000000`.

## What we did not copy from WALink

WALink (`davidsaltacc/walink`) is server-only, matches a group by display name on every message, bundles Node and runs `npm install` at launch, mixes system broadcasts into WhatsApp (echo risk), and can stall while syncing old history. This repo keeps the bridge as a separate process, disables history sync, binds the group id after one exact match, and does not log message text. Vanish is not implemented there either. Do not add it unless the user asks.

## Local files

Windows config is `%APPDATA%\whatsapp-mc\config.json`. Elsewhere it is `~/.config/whatsapp-mc/config.json`. `auth/` is the linked device. `lids.json` and `group.json` are next to it. None of these belong in git. `.gitignore` already excludes `**/config.json` except `bridge/config.example.json`.

Port and token changes need a bridge restart. Number, mode, and group name reload from the file watcher.

## Checks before you finish

1. `gradlew.bat buildAll` is green, including both client jars.
2. Bridge tests did not print a QR code.
3. Lite still swallows only the prefix, and Full does not swallow it (`ClientSessionTest`).
4. A lane `direct` frame does not appear in `GroupRelay`, and a lane `group` frame does not appear in `ChatController`.
5. No real phone number or token was added under the repo.

The GitHub repo is [Vabian124/whatsapp-in-minecraft](https://github.com/Vabian124/whatsapp-in-minecraft). Do not commit `auth`, `config.json`, or `node_modules`.

## Not done yet

- The QR is still printed in the bridge terminal, not drawn in a Minecraft screen.
- Full mode was compiled against the Fabric APIs above, but it has not been played in a live world with a real group.
- The settings screen does not reflow when the Full hint is wider than the field.
