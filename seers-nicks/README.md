# Seer's Nicks

A simple and opinionated nickname mod.

## Features

- **User-assigned Nicknames**: Allows users to set their own nicknames.
- **Basic Formatting**: Players can use basic formatting tags (colours) to enhance their names.
- **Easily Identifiable**: Nicknamed players are always prefixed with `≈` wherever they're seen.
- **Admin Controls**: Admins can set anyone's nickname, with any formatting, even offline.

## Usage

This mod is server-side.

All the functionality of this mod is driven by commands, listed below. Formatting uses the excellent
[Text Placeholder API][1] by Patbox. You can find more information about available tags and
formatting options in the [Text Placeholder API documentation][2]. Players can only use colours
(including `<rainbow>` and gradients), but admins can use any formatting.

Nicknames show up in place of a player's name in chat, death messages, advancements, and anywhere
else the server shows their name. Hovering over a nickname in chat still shows who it really is, and
the tab list shows their real name next to their nickname. Name tags above players' heads aren't
changed.

Nicknames can be up to 15 characters long, not counting formatting tags or the `≈`, and can't be
another player's username. Admins (permission level 2) can ignore the username rule, and can change
anyone's nickname, even if they're offline.

### Commands

| Command                                | Description                                     |
|----------------------------------------|-------------------------------------------------|
| `/nick set <nickname>`                 | Sets your nickname.                             |
| `/nick reset`                          | Resets your nickname to the default.            |
| `/nick player <player> set <nickname>` | Sets another player's nickname (admins only).   |
| `/nick player <player> reset`          | Resets another player's nickname (admins only). |

### Events

#### NicksEvents.CHANGED

Fired when a player's nickname has changed, providing both the old and new nickname. Only fired when
the nickname has actually changed. The player may be offline, if an admin changed it, so they're
identified by their UUID and username rather than as a player.

```
dev.seeruk.bundle.nicks.api.NicksEvents.CHANGED
```

**Callback signature:**

```java
void onNickChanged(MinecraftServer server, NameAndId player, Optional<Component> original, Optional<Component> edited);
```

| Parameter  | Description                                      |
|------------|--------------------------------------------------|
| `server`   | The server the nickname changed on.              |
| `player`   | The player whose nickname has changed.           |
| `original` | The player's original nickname, if they had one. |
| `edited`   | The player's new nickname, if they have one.     |

### Statistics

Statistics only count nicknames that have changed, and actually been set (not reset).

| Scoreboard Objective        | Description                                            |
|-----------------------------|--------------------------------------------------------|
| `seers_nicks.nicknames_set` | The number of times the player's nickname has been set |

Objectives can be removed while the server is running, but will be recreated on server restart.
Removing an objective will reset it for all players.

[1]: https://placeholders.pb4.eu/
[2]: https://placeholders.pb4.eu/user/text-format/
