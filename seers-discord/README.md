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
   Bot page. On the same page, turn on Message Content Intent, so the bot can read messages in its
   channel.
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

| Setting                 | Default                        | Description                                           |
|-------------------------|--------------------------------|-------------------------------------------------------|
| `bot_token`             | `""`                           | The bot's token. Keep it secret!                      |
| `channel_id`            | `""`                           | The channel to bridge chat with.                      |
| `formats.discord.chat`  | `${message}`                   | How chat messages are written in Discord.             |
| `formats.discord.emote` | `_${display_name} ${message}_` | How emotes (`/me`) are written in Discord.            |
| `formats.game.chat`     | See `config.dist.yml`          | How Discord messages are shown in game.               |
| `formats.game.reply`    | See `config.dist.yml`          | How what a Discord reply replied to is shown in game. |

Formats for Discord can use Discord's formatting (e.g. `_italics_`), `${message}` for the message,
and `${display_name}` for the player's name, as it's shown in chat.

Formats for the game can use the game's formatting tags (e.g. `<blue>`), `${message}` for the
message, `${display_name}` for the sender's name in the bot's server (e.g. their nickname), and
`${username}` for their Discord username. By default, the username is shown when hovering over
their name, as names in a server aren't unique. The chat format can also use `${reply}`, for where
the reply format goes if the message is a reply, which has the same placeholders, for the message
that was replied to.

### Chat in Discord

Chat messages and emotes are sent to Discord through a webhook the bot makes in its channel, so
they're shown as being from the player, with their name as chat shows it (e.g. with a nickname, or
a team prefix), and their face as the avatar. Players' faces come from [Mineatar][3].

Formatting from the game (bold, italics, underline, and strikethrough) is kept, and anything else
players write is shown in Discord exactly as they wrote it, so they can't use Discord's formatting
or mention anyone (e.g. with `@everyone`).

Any items linked in chat (e.g. via Seer's Chat's `<item>`) are shown as embeds under the relayed
message containing it. The tooltip contents are built into the embed, with the item name colour
used as the side colour for the embed. This includes all information included in the in-game tooltip
by default, and also a couple of elements from what would be shown in an advanced tooltip (e.g. 
durability). The item's own name is shown at the bottom, so it's clear what it is, even if it's been
renamed.

### Chat from Discord

Messages sent in the bot's channel are shown in game, by default like players' chat
(`<Seer> Hello!`), with the sender's name in blue, as it is in the Discord server (e.g. their
nickname). Hovering over their name shows their Discord username. Messages from bots and webhooks
aren't shown, so messages relayed from the game don't endlessly echo.

- **Formatting:** Discord's formatting is kept as well as the game can show it (e.g. bold, italics,
  links, and code), and anything else is shown as it was written, so tags like `<red>` don't work.
- **Spoilers:** these are shown as `[spoiler]`, with what's hidden shown when it's hovered over.
- **Mentions:** these are shown by name, e.g. `@Seer` or `#general`, in green.
- **Emoji:** ones the game can show are kept, and others are shown by name, e.g. `:smile:`, this 
  includes custom emojis.
- **Replies:** these start with a `➥`, which shows who and what was replied to when it's hovered
  over.
- **Attachments and stickers:** these are shown as `[image]`, `[file: notes.txt]`, or
  `[sticker: Wave]`. Attachments can be clicked to open them.
- **Long messages:** these are cut down to 256 characters and 5 lines, ending with `[...]`, which
  shows the whole message when it's hovered over (up to 1,000 characters and 20 lines, so it fits
  on screen).

### Events

This mod doesn't currently have any events.

### Statistics

This mod doesn't currently have any statistics.

[1]: https://jda.wiki/
[2]: https://discord.com/developers/applications
[3]: https://mineatar.io/
