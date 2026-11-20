# Seer's Discord

A mod that adds Discord integration to a server.

## Features

- **Bidirectional Chat**: In-game chat is sent to Discord, and Discord messages are sent to the 
  game, with as much of the formatting left intact as possible.
- **Server and Player Events**: Relays important server and player events to Discord; for example,
  server start/stop, player join/leave, achievements, deaths, etc.
- **Supports Linked Items**: Items linked in chat show up in Discord as embeds. Should work with 
  most mods that enable linking items in chat.
- **Slash Commands**: Adds `/playerlist` (and `/players`) to see who's online, and `/status` to 
  check in on the server performance from Discord.

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
2. Invite the bot to your Discord server, from the OAuth2 page: choose the `bot` and
   `applications.commands` scopes (for its slash commands), and the View Channels, Send Messages,
   Embed Links, and Manage Webhooks permissions.
3. Turn on Developer Mode in Discord's advanced settings, then right-click the channel to bridge
   chat with, and choose Copy Channel ID.
4. Set `bot_token` and `channel_id` in the config, and restart the server.

Replies to the bot's commands come from the bot itself, so its name and avatar are the ones set for
its application in the developer portal. Events from the game are sent with the server's own name
and avatar, which are set in the config (by default, "Minecraft Server", and the server's icon).

The server log says whether the bot connected, or what's wrong if it couldn't (e.g. a missing
permission). The server starts either way, and Discord stays off until it's set up.

### Config

Settings live in `config/seers-discord/config.yml` (see `config.dist.yml` next to it for every
setting and their defaults).

| Setting            | Default                        | Description                                                                  |
|--------------------|--------------------------------|------------------------------------------------------------------------------|
| `bot_token`        | `""`                           | The bot's token. Keep it secret!                                             |
| `channel_id`       | `""`                           | The channel to bridge chat with.                                             |
| `discord.server`   | See `config.dist.yml`          | The name and avatar events are sent with in Discord.                         |
| `discord.chat`     | `${message}`                   | How chat messages are written in Discord.                                    |
| `discord.emote`    | `_${display_name} ${message}_` | How emotes (`/me`) are written in Discord.                                   |
| `discord.events`   | See `config.dist.yml`          | Which events from the game are sent to Discord, and how they look.           |
| `discord.commands` | See `config.dist.yml`          | Which of the bot's slash commands are available, and who sees their replies. |
| `game.chat`        | See `config.dist.yml`          | How Discord messages are shown in game.                                      |
| `game.reply`       | See `config.dist.yml`          | How what a Discord reply replied to is shown in game.                        |

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
  Links are aqua and underlined, and open when clicked.
- **Spoilers:** these are shown as a dark grey `[spoiler]`, with what's hidden shown when it's
  hovered over.
- **Mentions:** these are shown by name, e.g. `@Seer` or `#general`, in green.
- **Emoji:** ones the game can show are kept, and others are shown by name, e.g. `:smile:`, this
  includes custom emojis.
- **Replies:** these start with a `➥`, which shows who and what was replied to when it's hovered
  over.
- **Attachments and stickers:** these are shown as `[image]`, `[file: notes.txt]`, or
  `[sticker: Wave]`. Attachments are aqua and underlined, like links, and open when clicked.
- **Long messages:** these are cut down to 256 characters and 5 lines, ending with a light purple
  `[...]`, which shows the whole message when it's hovered over (up to 1,000 characters and 20
  lines, so it fits on screen).

### Game Events in Discord

Things that happen in game are sent to the bot's channel as small embeds, as the server (with the
name and avatar in the config's `discord.server`), rather than a player, with the player's face
where there is one. By default, they're worded and coloured like the game shows them:

- **Joining and leaving:** e.g. "Seer joined the game", in yellow.
- **Deaths:** the game's death message, e.g. "Seer was slain by Zombie", in red. These are only sent
  when the game shows them in chat (i.e. with the `show_death_messages` game rule on).
- **Advancements:** the game's announcement, e.g. "Seer has made the advancement [Stone Age]", with
  the advancement's description below it, in green, or dark purple for challenges. Only
  advancements the game announces in chat are sent. Advancements completed at the same time are
  sent together, and if there are more than fit in one Discord message (e.g. from
  `/advancement grant @s everything`), one summary is sent instead, e.g. "Seer completed 112
  advancements".
- **Going AFK and coming back:** e.g. "Seer is now AFK", in yellow, if [Seer's AFK][4] is installed.
- **The server starting and stopping:** e.g. "🟢 The server has started", in green and red.

Each can be turned off, reworded, or recoloured in the config's `discord.events` section, using
Minecraft's colour names (e.g. `gold`) or hex (e.g. `#FFAA00`). See `config.dist.yml` for each
event's placeholders.

### Commands in Discord

The bot adds slash commands to its Discord server, which work in any of its channels:

| Command                    | Description                                                                                                                                                |
|----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/players` (`/playerlist`) | Shows who's online, as a bulleted list of their names as chat shows them, in alphabetical order. AFK players are listed last (if Seer's AFK is installed). |
| `/status`                  | Shows how long the server's been up, how many players are online, its version, and its TPS and MSPT, averaged over 1, 5, and 15 minutes.                   |

By default, replies are only shown to whoever ran the command. Who can use the commands, and where,
can be changed in the Discord server's settings, under Integrations.

### Events

This mod doesn't currently have any events.

### Statistics

This mod doesn't currently have any statistics.

[1]: https://jda.wiki/
[2]: https://discord.com/developers/applications
[3]: https://mineatar.io/
[4]: ../seers-afk
