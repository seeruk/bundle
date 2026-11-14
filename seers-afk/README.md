# Seer's AFK

A simple AFK mod.

## Features

- **AFK detection**: Automatically detects when players are AFK, uses the configured timeout.
- **AFK status**: Shows AFK status in the tab list.
- **AFK Command**: Allows players to toggle their AFK status.

## Usage

This mod is server-side.

There are no real surprises here. Admins configure a timeout in the config file. If players are idle
for that length of time then they're considered AFK and marked as such. AFK state is visible in the 
tab list, and a message is broadcast when a players AFK state changes. Players can mark themselves
as AFK immediately by using the `/afk` command.

### Config

Settings live in `config/seers-afk/config.yml` (see `config.dist.yml` next to it for every setting,
and its default).

| Setting           | Default                                       | Description                                                |
|-------------------|-----------------------------------------------|------------------------------------------------------------|
| `timeout_seconds` | `300`                                         | How long a player can do nothing before they're AFK.       |
| `messages.afk`    | `<yellow>${player} is now AFK</yellow>`       | Shown to everyone when a player goes AFK.                  |
| `messages.back`   | `<yellow>${player} is no longer AFK</yellow>` | Shown to everyone when a player comes back.                |

Messages can use any of [Text Placeholder API's formatting][1], and `${player}` is replaced with the
player's name, as it's shown in chat (so it includes any nickname or team colours).

### Commands

| Command | Description              |
|---------|--------------------------|
| `/afk`  | Toggles your AFK status. |

### Events

#### AfkEvents.STATUS_CHANGED

Fired when a player's AFK status changes, by any means. It's not fired when an AFK player
disconnects.

```
dev.seeruk.bundle.afk.api.AfkEvents.STATUS_CHANGED
```

**Callback signature:**

```java
void onStatusChanged(ServerPlayer player, boolean isAfk, AfkReason reason);
```

| Parameter | Description                                                     |
|-----------|-----------------------------------------------------------------|
| `player`  | The player whose AFK status changed.                            |
| `isAfk`   | The AFK status the player has changed to.                       |
| `reason`  | Why it changed: `TIMEOUT`, `ACTIVITY`, or `COMMAND` (`/afk`).   |

### Statistics

| Scoreboard Objective  | Description                                              |
|-----------------------|----------------------------------------------------------|
| `seers_afk.afk_ticks` | The number of ticks a player has been AFK for, in total. |

Objectives can be removed while the server is running, but will be recreated on server restart.
Removing an objective will reset it for all players.

[1]: https://placeholders.pb4.eu/user/text-format/
