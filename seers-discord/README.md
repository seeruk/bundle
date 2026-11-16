# Seer's Discord

A mod that adds Discord integration to a server.

## Features

- **Bidirectional Chat**: In-game chat is sent to Discord, and Discord messages are sent to the 
  game, with as much of the formatting left intact as possible.
- **Server and Player Events**: Relays important server and player events to Discord; for example,
  server start/stop, player join/leave, achievements, deaths, etc.
- **Supports Linked Items**: Items linked in chat show up in Discord as embeds. Should work with 
  most mods that enable linking items in chat.
- **Slash Commands**: Adds `/playerlist` to see who's online, and `/status` to check in on the 
  server performance from Discord.

## Usage

This mod is server-side.

The mod is entirely configuration driven, with no in-game commands or configuration options. Once
configured successfully, the mod will just do its thing!

Behind the scenes this mod uses the excellent [JDA][1] library, ensuring smooth integration with
Discord, including properly handling things like rate limits.

### Setup

1. Create an application in the [Discord developer portal][2], and copy its bot's token from the
   Bot page.
2. Invite the bot to your Discord server, from the OAuth2 page: choose the `bot` scope, and the View
   Channels, Send Messages, Embed Links, and Manage Webhooks permissions.
3. Turn on Developer Mode in Discord's advanced settings, then right-click the channel to bridge
   chat with, and choose Copy Channel ID.
4. Set `bot_token` and `channel_id` in the config, and restart the server.

The server log says whether the bot connected, or what's wrong if it couldn't (e.g. a missing
permission). The server starts either way, and Discord stays off until it's set up.

### Config

Settings live in `config/seers-discord/config.yml` (see `config.dist.yml` next to it for every
setting and their defaults).

| Setting         | Default                        | Description                      |
|-----------------|--------------------------------|----------------------------------|
| `bot_token`     | `""`                           | The bot's token. Keep it secret! |
| `channel_id`    | `""`                           | The channel to bridge chat with. |
| `formats.chat`  | `${message}`                   | How chat messages are written.   |
| `formats.emote` | `_${display_name} ${message}_` | How emotes (`/me`) are written.  |

Formats can use Discord's formatting (e.g. `_italics_`), `${message}` for the message, and
`${display_name}` for the player's name, as it's shown in chat.

### Chat in Discord

Chat messages and emotes are sent to Discord through a webhook the bot makes in its channel, so
they're shown as being from the player, with their name as chat shows it (e.g. with a nickname, or
a team prefix), and their face as the avatar. Players' faces come from [Mineatar][3].

Formatting from the game (bold, italics, underline, and strikethrough) is kept, and anything else
players write is shown in Discord exactly as they wrote it, so they can't use Discord's formatting
or mention anyone (e.g. with `@everyone`).

### Events

This mod doesn't currently have any events.

### Statistics

This mod doesn't currently have any statistics.

[1]: https://jda.wiki/
[2]: https://discord.com/developers/applications
[3]: https://mineatar.io/
