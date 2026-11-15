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
configured successfully, the mod will just do it's thing!

Behind the scenes this mod uses the excellent [JDA][1] library, ensuring smooth integration with 
Discord, including properly handling things like rate limits.

### Config

Settings live in `config/seers-discord/config.yml` (see `config.dist.yml` next to it for every 
setting and their defaults).

| Setting | Default | Description |
|---------|---------|-------------|
| TODO | | |

### Events

This mod doesn't currently have any events.

### Statistics

This mod doesn't currently have any statistics.

[1]: https://jda.wiki/
