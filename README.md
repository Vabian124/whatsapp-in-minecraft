# WhatsApp Chat

Chat with one WhatsApp contact from inside Minecraft, or switch the same mod to share a world's chat with one WhatsApp group.

**Lite is the default.** Their messages show up in your own chat, and a short prefix sends your reply back. The Minecraft server never receives that private thread. **Full** is the other position of the same switch: the world you are hosting shares ordinary player chat with one WhatsApp group.

**Minecraft 26.3 is the main version.** A separate jar is also built for 1.21.11. Install only the jar that matches your game.

This is a client mod for Lite. The person you message stays on normal WhatsApp and installs nothing. Full mode also loads on a dedicated server, because that is where the shared world chat lives. Other players still install nothing.

## What you need

- Minecraft Java 26.3 with Fabric Loader 0.19.5 or newer, and Fabric API. Java 25.
- Or Minecraft 1.21.11 with the same loader and Fabric API. Java 21.
- Node.js 22 or newer, on the same computer, for the bridge.
- Your own WhatsApp account. You link this computer once, the same way as WhatsApp Web.

## Install

1. Install Fabric for your Minecraft version, then put Fabric API in `.minecraft/mods`.
2. Put the matching mod jar in `.minecraft/mods`, and install [Mod Menu](https://modrinth.com/mod/modmenu) for the same Minecraft version if you want Esc → Mods → WhatsappMC.
   - 26.3: `fabric-26/build/libs/whatsappchat-1.0.0+26.3.jar`
   - 1.21.11: `fabric-121/build/libs/whatsappchat-1.0.0+1.21.11.jar`
3. In this project, start the bridge and leave that window open while you play:

```bat
cd bridge
npm install
npm start
```

The first launch creates `%APPDATA%\whatsapp-mc\` on Windows (or `~/.config/whatsapp-mc/` elsewhere). That folder holds the link token and the WhatsApp session. It is not part of the mod jar, and it should not be committed or uploaded.

## First-time setup

1. Start the bridge. The first time, it prints a QR code.
2. On your phone open WhatsApp, then **Linked devices**, then **Link a device**, and scan that code.
3. In Minecraft press **Esc**, **Mods**, **WhatsappMC**, then the config button. `/whatsapp` opens the same screen if Mod Menu is not installed.
4. Choose **Lite** for one private chat, or **Full** to share a hosted world's chat with a WhatsApp group.
5. In Lite, fill in:
   - **Name** is what you see in Minecraft chat.
   - **Number** is their international number. Country code, no plus. A Dutch mobile `+31 6 12 34 56 78` is `31612345678`.
   - **Prefix** is what your replies start with. The default is `.h `, including the space.
6. In Full, type the WhatsApp group name exactly.
7. Press **Save**. The bridge picks up the new number or group without a restart.

The screen also shows whether the bridge is running, waiting for the QR, or connected.

## Talking

Open Minecraft chat and type the prefix plus the message:

```text
.h I'm in a cave, one minute
```

That line is cancelled before it is sent to the server. A normal chat line, one that does not start with the prefix, still goes to the server as usual. Do not start the prefix with `/`. Slash lines are commands, and this mod will not swallow them.

Their texts appear in green. Yours appear in gray after the bridge accepts them. Photos, voice notes, stickers, and files are not shown. You get a short placeholder such as `[photo]` instead.

The vanilla chat box limits each reply to 256 characters. Split a longer reply yourself.

## Settings

Press **Esc**, then **Mods**, then **WhatsappMC**, then the config button. That screen is provided by [Mod Menu](https://modrinth.com/mod/modmenu). Install Mod Menu next to this mod. It is optional: without it, type `/whatsapp` instead. You can also bind **Open WhatsApp settings** in Controls. The key starts unbound.

The screen has two modes:

- **Lite** is one private chat. Fill in the name, the international number, and the reply prefix. The default prefix is `.h `, including the space.
- **Full** shares the chat of a world you host with one WhatsApp group. The group name has to match exactly. On a dedicated server an operator can also run `/whatsapp full`, `/whatsapp lite`, and `/whatsapp group <name>`.

Saving writes `%APPDATA%\whatsapp-mc\config.json` on Windows, or `~/.config/whatsapp-mc/config.json` elsewhere. You can also edit that file while the bridge is running. Changing the phone number, the mode, or the group name applies live. Changing the port or the token needs a bridge restart.

Lite forwards only that one 1:1 chat. Full forwards only the named group. Everything else is ignored.

In Full mode, a message in the WhatsApp group that is exactly `.mc players` replies with who is online. `.mc help` lists those commands. Normal group text is shown in the world chat. Your own player chat is sent to the group. Commands that start with `/` stay in Minecraft.

## Privacy

- Lite is private to your game. A dedicated server does not see that thread, and message text is not written to a log.
- Full mode puts the shared world chat on the computer that hosts the world. That copy is not end-to-end encrypted. Use it for a small group you already trust.
- WhatsApp text is drawn as plain text. It is not parsed as a Minecraft click event or command.
- The bridge listens on `127.0.0.1` only and checks a local token.
- The `auth` folder is the key to the linked device. Anyone with that folder can use the link. Keep it on your user account.

## Limits

WhatsApp does not offer a personal API. The official Business Platform is a separate business number, with opt-in and template rules, so it cannot sit on your normal chats. This bridge uses a linked device through [`@whiskeysockets/baileys`](https://github.com/WhiskeySockets/Baileys). WhatsApp's terms restrict unauthorized automated access, and automated use can restrict an account. Use it for one low-volume personal chat. Do not use it to message people you do not know.

WhatsApp sometimes addresses a chat with a hidden id instead of the phone number. The bridge remembers the id from their latest message and replies to that. Send them one message from the phone, or wait for one from them, before the first in-game reply after a new link.

## Build

Java 25 is required to compile the 26.3 mod. The 1.21.11 jar is compiled for Java 21 from the same shared code.

```bat
gradlew.bat buildAll
```

That compiles both jars, runs the shared Java tests, and lints plus dry-runs the bridge. The dry run talks to a localhost socket only. It does not link WhatsApp.

```bat
cd bridge
npm run check
```

## Modrinth

When you publish, upload the **26.3** jar as the primary version and the 1.21.11 jar as its own version. The summary can be:

> Read and reply to one WhatsApp contact from Minecraft chat. Set the contact in-game with /whatsapp. Client-side. Requires the included local bridge.

Required dependency: Fabric API. The bridge is not a mod and is not uploaded as one. Tell players to run `npm start` inside `bridge` from this repository.

## Layout

- `shared` — protocol, settings, and chat rules, with no Minecraft imports
- `client-common` — the Mod Menu hook, compiled into both jars
- `server-common` — Full mode, compiled into both jars
- `fabric-26` — Minecraft 26.3 screens and the 26.3 operator check
- `fabric-121` — the same for 1.21.11
- `bridge` — WhatsApp session and localhost socket
- `LLM.md` — notes for the next person or agent changing this repo

## License

[MIT](LICENSE)
