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

| Command                            | Description                                             |
|------------------------------------|---------------------------------------------------------|
| `/itemname set <name>`             | Sets the name of an item                                |
| `/itemname reset`                  | Resets the name of an item to its default               |
| `/itemlore`                        | Shows the lore of an item, with line numbers            |
| `/itemlore add <lore>`             | Adds a line to the end of an item's lore                |
| `/itemlore set <line> <lore>`      | Sets a line of an item's lore                           |
| `/itemlore insert <line> [<lore>]` | Inserts a line of lore, moving the lines below it down  |
| `/itemlore remove [<line>]`        | Removes a line of lore, or all lore if no line is given |
| `/itemlore clear <line>`           | Clears a line of lore, leaving an empty line behind     |

### Limits

Limits are based on plain text, so formatting tags don't count towards them (e.g. `<red>Sword</red>`
counts as 5 characters).

| Limit                        | Value |
|------------------------------|-------|
| Maximum item name length     | 50    |
| Maximum number of Lore lines | 10    |
| Maximum lore line length     | 60    |

### Events

#### ItemNameEvents.EDITED

Fired when an item's name is set or reset, providing both the old and new name. Only fired when the
name has actually changed.

```
dev.seeruk.bundle.itemtext.api.ItemNameEvents.EDITED
```

**Callback signature:**

```java
void onEdited(Player player, ItemStack item, Optional<Component> original, Optional<Component> edited);
```

| Parameter  | Description                                      |
|------------|--------------------------------------------------|
| `player`   | The player that modified the name                |
| `item`     | The item that had its name modified              |
| `original` | The original custom name of the item, if present |
| `edited`   | The new custom name of the item, if present      |

#### ItemLoreEvents.EDITED

Fired when an item's lore is modified in any way, providing both the old and new lore. Only fired
when the lore has actually changed.

```
dev.seeruk.bundle.itemtext.api.ItemLoreEvents.EDITED
```

**Callback signature:**

```java
void onEdited(Player player, ItemStack item, List<Component> original, List<Component> edited);
```

| Parameter  | Description                                   |
|------------|-----------------------------------------------|
| `player`   | The player that modified the lore             |
| `item`     | The item that had its lore modified           |
| `original` | The original lore of the item, before editing |
| `edited`   | The new lore of the item, after editing       |

### Statistics

**Planned, not implemented yet!** These statistics don't exist yet, but this is what they'll look
like when they do.

Statistics will only count new text written by players, i.e. the plain text of a new name, or of new
lines of lore. Removing names, or removing or clearing lines of lore won't affect them.

| Scoreboard Objective           | Description                                                       |
|--------------------------------|-------------------------------------------------------------------|
| `seers-item-text.name_written` | The total number of characters a player has written in item names |
| `seers-item-text.lore_written` | The total number of characters a player has written in item lore  |

[1]: https://placeholders.pb4.eu/
[2]: https://placeholders.pb4.eu/user/text-format/
