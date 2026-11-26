# Seer's Leaders

An in-world leaderboard for your server using mannequins, supporting any stats you configure!

Vanilla keeps track of loads of stats for every player, but there's nowhere to actually see how
players compare. This mod shows the top players for whatever you choose (blocks mined, distance
travelled, mobs slain, and scores from other mods) as mannequins wearing the leader's skin, placed
wherever you like. They keep themselves up to date, so a spawn area or town hall can show who's
leading right now.

Over the life of a long-running server, whoever's played longest can become untouchable, so
categories can also count stats over a period instead, e.g. this month. Those start again from
nothing when the period ends, and their winners are announced.

## Features

- **Configurable Categories**: Rank players by any Vanilla stats or scoreboard objectives, added up,
  with support for wildcards and exclusions.
- **Periodic Leaders**: Categories can count stats over a configurable period (e.g. weekly or
  monthly), so servers running for a long time can stay fresh.
- **Announcements**: New leaders can be announced in chat as players trade ranks, and periodic
  categories' winners are announced at the end of their configured period.
- **Leader Mannequins**: Admins can place mannequins wearing the skin of whoever holds a rank, named
  after their category, also showing their total, and optionally holding an item that fits the
  category.

## Usage

This mod is server-side.

Start by looking over the default categories and periods in `config.dist.yml` (see
[Config](#config)). There are a bunch of categories available out of the box which showcase the
available configuration options. Copy any you want to keep or change into `config.yml`, and add your
own. When the server starts, a warning is logged for any stat name that doesn't exist (e.g. a typo).

Leaders are worked out from every player's stats, whether they're online or not, at a configured
interval. When someone overtakes a category's leader, it can be announced to everyone online. Being
the first to score in a category isn't announced, and leaders changing at the same time are
announced together in one message, so announcements don't get too spammy!

In a long-lived server, categories that track statistics for all time can be dominated by the same
players, maybe even if they've not played for a long time. Periodic categories help you get around
this by showcasing leaders across a period of time (say, a month), so only activity during that
period is counted. You can also show all-time and periodic leaders side-by-side. When a period ends
its winners are announced. The new period starts immediately, so until someone takes a rank in it,
that rank's mannequins name whoever held it last period (if the category has a
`previous_description`).

A period that ends while the server is stopped ends when the server next starts. Nobody will be
online at this point, so its announcement probably won't be seen (but its event is still fired). If
whole periods were missed while the server was stopped, its winners aren't announced at all to avoid
any confusion (imagine a server stopped for 2 years, and when it next starts it makes an
announcement about last month's winners).

Finally, place mannequins to show the leaders. Look at the block you want one on, and run
`/leaders place <category> [rank]`. It's placed on top of that block, facing you (snapped to 45°),
wearing the skin of whoever holds that rank, with their name (or nickname, with [Seer's Nicks][1])
and total. The rank defaults to 1, but you could show a typical podium with three mannequins with
ranks 1, 2, and 3. To remove one, look at it and run `/leaders remove`.

Mannequins can't be hurt or pushed by players in survival, but players in creative can still break
them, as with any Vanilla mannequin. They only remember which category and rank they show, so
changing a category in the config changes every mannequin showing it (one whose category is
removed is left as it is, and a warning is logged). They're updated while they're loaded, and catch
up as soon as they load, so no work is done for mannequins nobody's near.

### Config

Settings live in `config/seers-leaders/config.yml` (see `config.dist.yml` next to it for every
setting, and their defaults).

| Setting                       | Default               | Description                                                             |
|-------------------------------|-----------------------|-------------------------------------------------------------------------|
| `refresh_seconds`             | `300`                 | How often leaders are worked out again, in seconds.                     |
| `timezone`                    | The server's timezone | The time zone periods' schedules are in, e.g. `Europe/London`.          |
| `announcement.enabled`        | `true`                | Whether new leaders are announced.                                      |
| `announcement.format`         | See `config.dist.yml` | Sent when a category has a new leader.                                  |
| `period_announcement.enabled` | `true`                | Whether each period's winners are announced when it ends.               |
| `period_announcement.header`  | See `config.dist.yml` | The first line sent when a period ends.                                 |
| `period_announcement.line`    | See `config.dist.yml` | A line for each of the period's categories that someone won.            |
| `periods`                     | See `config.dist.yml` | The periods available to use: what period they span, and when they end. |
| `categories`                  | See `config.dist.yml` | What players are ranked by (see below).                                 |

Setting `periods` or `categories` in `config.yml` replaces the whole list, so copy any of the
defaults you want to keep.

#### Categories

| Setting                | Description                                                                                        |
|------------------------|----------------------------------------------------------------------------------------------------|
| `id`                   | The category name used in commands and internally on mannequins showing it. Should be kept stable. |
| `title`                | The category name as it appears to players, as `${category}`.                                      |
| `period`               | Only counts stats from the given period, if set (optional).                                        |
| `stats`                | The stats added up for each player (see [Stat Patterns](#stat-patterns)).                          |
| `unit`                 | How the total is shown: `count`, `distance` (blocks), or `time` (hours).                           |
| `name`                 | A mannequin's name, with `${player}`, `${value}`, `${rank}`, and `${category}`.                    |
| `description`          | Shown under a mannequin's name, with the same placeholders.                                        |
| `empty_name`           | A mannequin's name when nobody holds its rank, with `${rank}` and `${category}`.                   |
| `empty_description`    | Shown under it when nobody holds its rank.                                                         |
| `previous_description` | Shown instead, when someone held the rank when the last period ended (optional).                   |
| `announce_changes`     | Whether new leaders are announced (optional, `true` by default).                                   |
| `item`                 | What mannequins hold, e.g. `minecraft:diamond_pickaxe` (optional).                                 |

#### Periods

| Setting | Description                                                                                  |
|---------|----------------------------------------------------------------------------------------------|
| `id`    | The name used to refer to this period in a category.                                         |
| `title` | The name as it appears to players, as `${period}`.                                           |
| `every` | `day`, `week`, or `month`.                                                                   |
| `on`    | For `week`, a day (e.g. `sunday`). For `month`, a day of the month (`1` to `31`), or `last`. |
| `at`    | The time it ends, e.g. `"18:00"`.                                                            |

It's usually better to pick a time when players are around, so they see the announcement. Changing a
period's schedule starts it again from nothing, without announcing anything, and removing a period
discards its progress (a warning is logged when the server starts).

#### Stat Patterns

Vanilla stats use their scoreboard criterion names, and scoreboard objectives are
`scoreboard:<objective>`. A `*` matches anything, and a pattern starting with `!` leaves out
whatever it matches. Each stat is only counted once, regardless of how many patterns match it.

| Pattern                                   | Matches                                       |
|-------------------------------------------|-----------------------------------------------|
| `minecraft.mined:minecraft.stone`         | Stone mined.                                  |
| `minecraft.mined:*`                       | Every block mined.                            |
| `minecraft.custom:minecraft.*_one_cm`     | Every way of moving (walking, swimming, etc.) |
| `!minecraft.custom:minecraft.fall_one_cm` | Leaves out distance fallen.                   |
| `minecraft.killed:*`                      | Every mob killed.                             |
| `scoreboard:seers_chat.messages_sent`     | Messages sent, from [Seer's Chat][2].         |

Any mod's stats or scoreboard objectives can be used this way, without the mods needing to know
about each other. A warning is logged when the server starts for any exact name (without a `*`)
that isn't a stat or objective, e.g. a typo, or an objective from a mod that isn't installed.

### Commands

Commands are only for admins (permission level 2).

| Command                            | Description                                                                        |
|------------------------------------|------------------------------------------------------------------------------------|
| `/leaders place <category> [rank]` | Places a mannequin showing the rank (1 by default) on the block you're looking at. |
| `/leaders remove`                  | Removes the leader mannequin you're looking at.                                    |

### Events

#### LeaderEvents.CHANGED

Fired when a category's leader changes, including to nobody, when a period starts again. It's not
fired the first time a category is ranked, as there's nobody to compare with.

```
dev.seeruk.bundle.leaders.api.LeaderEvents.CHANGED
```

**Callback signature:**

```java
void onLeaderChanged(MinecraftServer server, LeaderEvents.Change change);
```

| Field                 | Description                                                                        |
|-----------------------|------------------------------------------------------------------------------------|
| `change.category`     | The category's ID.                                                                 |
| `change.title`        | The category's title, as players see it.                                           |
| `change.previous`     | Who led the category before, if anyone.                                            |
| `change.leader`       | Who leads the category now, if anyone.                                             |
| `change.announceable` | Whether it's worth announcing (an overtake, in a category that announces changes). |

#### LeaderEvents.PERIOD_ENDED

Fired when a period ends, with the winner of each of its categories, just before the next period
starts. It's fired even when the winners aren't announced.

```
dev.seeruk.bundle.leaders.api.LeaderEvents.PERIOD_ENDED
```

**Callback signature:**

```java
void onPeriodEnded(MinecraftServer server, LeaderEvents.PeriodEnd end);
```

| Field              | Description                                                                                         |
|--------------------|-----------------------------------------------------------------------------------------------------|
| `end.period`       | The period's ID.                                                                                    |
| `end.title`        | The period's title, as players see it.                                                              |
| `end.winners`      | Who won each of the period's categories, with the category's title, and their total (and as shown). |
| `end.announceable` | Whether it's worth announcing (someone won something, and no whole periods were missed).            |

### Statistics

This mod doesn't add any statistics of its own, it only reads them.

[1]: ../seers-nicks
[2]: ../seers-chat
