# Seer's Chat

A simple and opinionated chat mod, adding formatting, item links, and handy tags to Vanilla's chat.

## Features

- **Formatting**: Players can format their messages (colours, bold, italics, underline, etc.)
- **Linking Items**: Players can link the items they're holding or wearing in their messages.
- **Utility Tags**: Players can share where they are, where they died, their health, and more.
- **Trusted Players**: Operators can use every kind of formatting, and placeholders.
- **Emotes**: Emotes sent with Vanilla's `/me` are shown in grey italics.

## Usage

This mod is server-side.

For the most part, just use chat like normal! Messages support the same safe subset of
[Text Placeholder API's formatting][1] as item names do, so nothing that clicks, hovers, or runs
commands. Formatting works in any message a player writes, e.g. in chat, `/me`, or `/msg`.

Players who can use commands like `/say` (by default, operators) are trusted, so they can use every
tag, including clicks and hovers, and [placeholders][2] (e.g. `%player:name%`).

Chat itself is left to Vanilla, so names (including nicknames) are shown as normal, and chat stays
secure. Formatted messages are marked as changed by the server in chat, and players who only show
secure chat will see them as they were written instead.

### Tags

| Tag                                     | Shows                                       |
|-----------------------------------------|---------------------------------------------|
| `<item>`                                | A link to the item in your main hand.       |
| `<mainhand>`, `<offhand>`               | A link to the item in that hand.            |
| `<head>`, `<chest>`, `<legs>`, `<feet>` | A link to the armour in that slot.          |
| `<item head>`, etc.                     | The same as the slot tags above.            |
| `<pos>`                                 | Your coordinates, e.g. `[100, 64, -20]`.    |
| `<death>`                               | Where you last died, like `<pos>`.          |
| `<dim>`                                 | The dimension you're in, e.g. `The Nether`. |
| `<biome>`                               | The biome you're in.                        |
| `<health>`                              | Your health, e.g. `❤ 14/20`.               |
| `<level>`                               | Your experience level, e.g. `Level 30`.     |

Items show their tooltip when hovered over. Coordinates show their dimension when hovered over, and
copy them when clicked. Tags with nothing to show (e.g. an empty hand) are left as written.

### Events

This mod doesn't currently have any events, as Fabric API already has
`ServerMessageEvents.CHAT_MESSAGE` which will have broader compatibility and usability. Anything
picking up on that event should work with this mod.

### Statistics

| Scoreboard Objective            | Description                                                 |
|---------------------------------|-------------------------------------------------------------|
| `seers_chat.messages_sent`      | The total number of messages sent by players.               |
| `seers_chat.message_characters` | The total number of characters in messages sent by players. |
| `seers_chat.emotes_sent`        | The total number of emotes sent by players.                 |
| `seers_chat.emote_characters`   | The total number of characters in emotes sent by players.   |

Characters are counted as shown, so formatting tags don't count. Objectives can be removed while the
server is running, but will be recreated on server restart. Removing an objective will reset it for
all players.

[1]: https://placeholders.pb4.eu/user/text-format/
[2]: https://placeholders.pb4.eu/user/default-placeholders/
