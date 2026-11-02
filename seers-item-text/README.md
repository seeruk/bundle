# Seer's Item Text

Adds player utilities for editing item text, including support for formatting markup and tags.

## Features

- **Item Text Commands**: Commands any player can run to edit an item's name and lore.
- **Formatting Support**: Support for formatting markup and tags, including allowing setting colour,
  bold, italic, and more.

## Usage

This mod is server-side.

All the functionality of this mod is driven by commands, listed below. Formatting uses the excellent
[Text Placeholder API][1] by Patbox. You can find more information about available tags and 
formatting options in the [Text Placeholder API documentation][2].

### Commands

| Command                            | Description                                            |
|------------------------------------|--------------------------------------------------------|
| `/itemname set <name>`             | Sets the name of an item                               |
| `/itemname reset`                  | Resets the name of an item to it's default             |
| `/itemlore`                        | Shows the lore of an item, with line numbers           |
| `/itemlore add <lore>`             | Adds a line to the end of an item's lore               |
| `/itemlore set <line> <lore>`      | Sets the lore of an item                               |
| `/itemlore insert <line> [<lore>]` | Inserts a line of lore, moving the lines below it down |
| `/itemlore remove <line>`          | Removes a line of lore, moving the lines below it up   |
| `/itemlore clear <line>`           | Clears a line of lore, leaving an empty line behind    |
| `/itemlore remove`                 | Removes all the lore from an item                      |

### Events

#### NAME_WRITTEN

Fired when an item's name is set (only when text is added, so, not when it's reset).

```
dev.seeruk.bundle.itemtext.api.ItemTextEvents.NAME_WRITTEN
```

**Callback signature:**

```java
void onNameWritten(Player player, ItemStack item, String name);
```

| Parameter | Description                    |
|-----------|--------------------------------|
| `player`  | The player who set the name    |
| `item`    | The item that had its name set |  
| `name`    | The name that was set          |

#### LORE_WRITTEN

Fired when an item's lore is added to (add, set, insert), but not when it's removed or cleared.

```
dev.seeruk.bundle.itemtext.api.ItemTextEvents.LORE_WRITTEN
```

**Callback signature:**

```java
void onLoreWritten(Player player, ItemStack item, int line, String lore);
```

| Parameter | Description                     |
|-----------|---------------------------------|
| `player`  | The player who set the lore     |
| `item`    | The item that had its lore set  |
| `line`    | The line number that was set on |
| `lore`    | The lore that was set           |

(All Lore can subsequently be retrieved from the item itself).

### Statistics

| Scoreboard Objective           | Description                                                       |
|--------------------------------|-------------------------------------------------------------------|
| `seers-item-text.name_written` | The total number of characters a player has written in item names |
| `seers-item-text.lore_written` | The total number of characters a player has written in item lore  |

[1]: https://placeholders.pb4.eu/
[2]: https://placeholders.pb4.eu/user/text-format/
