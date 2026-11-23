# Seer's Resource Zones

Allows admins to mark groups of chunks as resource zones, signalling to players that they'll be
reset, and making it easy for admins to reset them.

But why? In the past I made a "Resource Worlds" mod. That mod added separate copies of each Vanilla
dimension that players could teleport into, and destroy for resources as they saw fit. There's 
another nice benefit they gave, which is that they also gave access to new blocks and biomes without
needing to reset anything in the regular dimensions.

The problem is, they depend heavily on having teleport / home / back mods or functionality. Without
that they don't really work. It's difficult for players to get around in these worlds, but because 
players know these worlds are temporary they aren't willing to build efficient transport routes 
between locations in them. I also had to disable portals because they wouldn't link up in an 
expected way.

Resource _zones_ alleviate these issues. They're areas in the regular Vanilla dimensions. They're 
easy to reset, but they're static. Players can, and may _want to_ build transport links to them 
because while the zone contents are ephemeral, the location will remain static.

This mod aims to make regular areas in the Vanilla dimensions easy to mark and reset for admins, and
easy for players to identify. They'll be static once defined (by admins), and therefore players can
build worthwhile transport links to them (e.g. nether tunnels, so on).

## Features

- **Marked Zones**: Players are told when they're in one by a notice in their action bar, and can
  turn on particles to see where zones' edges are.
- **MCA Selector CSV Export**: Resource zones are automatically written to CSV files on the server
  whenever they change, ready to be used with MCA Selector.

### Usage

#### For Players

While you're in a resource zone, a notice is kept in your action bar, so you know that anything you
build there will be reset eventually. Resource zones are otherwise normal areas, so you can still
build, place beds, and set your respawn point in them.

To see where zones are, turn on particles with `/resourcezones particles <low|medium|high>`. The
edges of zones near you are then outlined, either as a short band at your height, or as a taller
wall with `/resourcezones particles style wall`. Particles are off until you turn them on, and
your choice is remembered.

#### For Admins

Add or remove single chunks with `/resourcezones add` and `/resourcezones remove`, which act on the
chunk you're in, or the one with the given block `x z`.

To change lots of chunks at once, edit zones by flying over chunks. Start with
`/resourcezones edit add` (or `remove`), and every chunk you fly over is marked to be added (or
removed). Flying back over a chunk in the other mode undoes it. While editing, the middle of each
chunk near you that's in a zone, or being added or removed, is marked with particles (each kind
with its own colour and shape), and your action bar shows how many chunks you've changed. Use
`/resourcezones edit pause` to look over your changes, then `commit` or `discard` them. Changes
aren't saved until they're committed, and are thrown away if you leave the server. Editing doesn't
count towards your zone statistics or events.

Whenever zones change (and when the server starts), each dimension's zone chunks are written to a
file as an MCA Selector selection, e.g. `resource-zones/minecraft/overworld.csv`. The mod never
resets anything itself. To reset the zones, stop the server, delete the selected chunks in each
dimension with [MCA Selector][1], then start the server again. With MCA Selector's command line
mode, that's:

```
java -jar mcaselector.jar --mode delete --world <world>/dimensions/minecraft/overworld --selection resource-zones/minecraft/overworld.csv
```

Next time a player visits, the chunks will regenerate fresh.

### Config

Settings live in `config/seers-resource-zones/config.yml` (see `config.dist.yml` next to it for
every setting, and their defaults).

| Setting                        | Default               | Description                                                           |
|--------------------------------|-----------------------|-----------------------------------------------------------------------|
| `notice`                       | See `config.dist.yml` | Kept in the action bar while a player is in a resource zone.          |
| `particles.colour`             | `gold`                | The particles' colour, as a colour name (e.g. `gold`), or hex.        |
| `particles.radius_blocks`      | `16`                  | How many blocks around each player (horizontally) zones are outlined. |
| `edit.status`                  | See `config.dist.yml` | Kept in the action bar while editing zones.                           |
| `edit.markers.radius_chunks`   | `8`                   | How many chunks around an editing admin are marked.                   |
| `edit.markers.colours.zone`    | `blue`                | The colour of markers on chunks in a zone that aren't changing.       |
| `edit.markers.colours.added`   | `green`               | The colour of markers on chunks being added.                          |
| `edit.markers.colours.removed` | `red`                 | The colour of markers on chunks being removed.                        |
| `export.folder`                | `resource-zones`      | The folder zones are exported to, inside the server's folder.         |

Going too much higher than 16 blocks around a player to show particles can result in hitting the 
client particle cap when using wall + high particles, and they'll stop showing correctly in weird 
ways.

### Commands

| Command                                             | Description                                                       |
|-----------------------------------------------------|-------------------------------------------------------------------|
| **Players**                                         |                                                                   |
| `/resourcezones particles`                          | Shows your particle settings.                                     |
| `/resourcezones particles <off\|low\|medium\|high>` | Sets how many particles outline zones for you, or turns them off. |
| `/resourcezones particles style <band\|wall>`       | Sets whether particles are a short band, or a taller wall.        |
| **Admins**                                          |                                                                   |
| `/resourcezones add [x z]`                          | Adds the chunk you're in (or the one with block x z) to zones.    |
| `/resourcezones remove [x z]`                       | Removes the chunk you're in (or the one with block x z).          |
| `/resourcezones edit`                               | Shows what you're editing.                                        |
| `/resourcezones edit <add\|remove>`                 | Starts adding or removing the chunks you fly over.                |
| `/resourcezones edit pause`                         | Stops marking chunks, keeping your changes.                       |
| `/resourcezones edit <commit\|discard>`             | Saves or throws away your changes, and stops editing.             |

From the console, `x z` must be given, and chunks are in the overworld unless the command is run
with `/execute in <dimension> run ...`.

### Events

#### ResourceZoneEvents.CHANGED

Fired when chunks are added to or removed from a dimension's resource zones. It's only fired when
they've actually changed.

```
dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents.CHANGED
```

**Callback signature:**

```java
void onChanged(MinecraftServer server, ResourceKey<Level> dimension);
```

#### ResourceZoneEvents.PLAYER_ENTERED and PLAYER_LEFT

Fired when a player goes into a resource zone from outside one, or out of one, however they got
there (e.g. walking, teleporting, or respawning). Moving between zone chunks isn't entering, and
neither joining nor leaving the server while in a zone counts as entering or leaving it. Zones being
added or removed around a player doesn't count either (e.g. an admin adding the chunk they're in),
and spectators and players editing zones are left out entirely.

```
dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents.PLAYER_ENTERED
dev.seeruk.bundle.resourcezones.api.ResourceZoneEvents.PLAYER_LEFT
```

**Callback signature:**

```java
void onPlayerMoved(ServerPlayer player);
```

### Statistics

| Scoreboard Objective                | Description                                                       |
|-------------------------------------|-------------------------------------------------------------------|
| `seers_resource_zones.zone_ticks`   | The total number of ticks a player has spent in resource zones.   |
| `seers_resource_zones.zone_entries` | The total number of times a player has entered a resource zone.   |
| `seers_resource_zones.blocks_mined` | The total number of blocks a player has broken in resource zones. |

Spectators and players editing zones don't count towards zone ticks or entries.

Objectives can be removed while the server is running, but will be recreated on server restart.
Removing an objective will reset it for all players.

[1]: https://github.com/Querz/mcaselector
