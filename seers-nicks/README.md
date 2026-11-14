# Seer's Nicks

A simple and opinionated nickname mod.

## Features

- **User-assigned Nicknames**: Allows users to set their own nicknames.
- **Basic Formatting**: Players can use basic formatting tags (colours) to enhance their names.
- **Easily Identifiable**: Nicknamed players are always prefixed with `≈` wherever they're seen.

## Usage

This mod is server-side.

All the functionality of this mod is driven by commands, listed below. Formatting uses the excellent
[Text Placeholder API][1] by Patbox. You can find more information about available tags and
formatting options in the [Text Placeholder API documentation][2]. Note that only colour-based
formatting is supported.

### Commands

| Command            | Description                          |
|--------------------|--------------------------------------|
| `/nick <nickname>` | Sets your nickname.                  |
| `/nick reset`      | Resets your nickname to the default. |

### Events

#### NicksEvents.CHANGED

Fired when a player's nickname has changed, providing both the old and new nickname. Only fired when
the nickname has actually changed.

```
dev.seeruk.bundle.nicks.api.NicksEvents.CHANGED
```

**Callback signature:**

```java
void onNickChanged(Player player, Optional<Component> original, Optional<Component> edited);
```

| Parameter  | Description                            |
|------------|----------------------------------------|
| `player`   | The player whose nickname has changed. |
| `original` | The player's original nickname.        |
| `edited`   | The player's new nickname.             |

### Statistics

Statics only count nicknames that have changed, and actually been set (not reset).

| Scoreboard Objective        | Description                                            |
|-----------------------------|--------------------------------------------------------|
| `seers-nicks.nicknames_set` | The number of times the player's nickname has been set |

Objectives can be removed while the server is running, but will be recreated on server restart.
Removing an objective will reset it for all players.

[1]: https://placeholders.pb4.eu/
[2]: https://placeholders.pb4.eu/user/text-format/
