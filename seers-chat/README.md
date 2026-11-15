# Seer's Chat

A simple and opinionated chat mod, primarily aiming to add functionality to the chat system, for 
example, basic formatting, and linking items.

## Features

- **Basic Formatting**: Add basic formatting to messages (colours, bold, italics, underline, etc.)
- **Linking Items**: Allow players to link items in chat messages. 
- **Emoting**: Allow players to "emote" via the `/me` command.

## Usage

This mod is server-side.

For the most part, just use chat like normal! This mod makes heavy use of the excellent 
[Text Placeholder API][1] by Patbox, allowing a wide range of [formatting options][2].

### Config

| Setting        | Default                                                       | Description               |
|----------------|---------------------------------------------------------------|---------------------------|
| `chat_format`  | `%player:displayname% <gray>»</gray> {text}`                  | The default chat format.  |
| `emote_format` | `<gray><italic>* %player:displayname% {text}</italic></gray>` | The default emote format. |

Configs using placeholders and formatting tags support the full range of [formatting options][2] 
provided by Text Placeholder API.

### Commands

| Command | Description                         |
|---------|-------------------------------------|
| `/me`   | Send an emote (a stylised message). |

### Events

#### ChatEvents.CHATTED

Fired when a player sends a message in chat.

```
dev.seeruk.bundle.chat.api.ChatEvents.CHATTED
```

**Callback signature:**

```java
void onChatted(ServerPlayer player, Component message)
```

| Parameter | Description                      |
|-----------|----------------------------------|
| `player`  | The player who sent the message. |
| `message` | The message sent by the player.  |

#### ChatEvents.EMOTED

Fired when a player emotes in chat.

```
dev.seeruk.bundle.chat.api.ChatEvents.EMOTED
```

**Callback signature:**

```java
void onEmoted(ServerPlayer player, Component message)
```

| Parameter | Description                      |
|-----------|----------------------------------|
| `player`  | The player who emoted.           |
| `message` | The emote sent by the player.    |

### Statistics

| Scoreboard Objective            | Description                                                 |
|---------------------------------|-------------------------------------------------------------|
| `seers_chat.emotes_sent`        | The total number of emotes sent by players.                 |
| `seers_chat.emote_characters`   | The total number of characters in emotes sent by players.   |
| `seers_chat.messages_sent`      | The total number of messages sent by players.               |
| `seers_chat.message_characters` | The total number of characters in messages sent by players. |

Objectives can be removed while the server is running, but will be recreated on server restart.
Removing an objective will reset it for all players.

[1]: https://github.com/Patbox/TextPlaceholderAPI
[2]: https://placeholders.pb4.eu/user/text-format/
