# Seer's Resource Zones

Allows registered groups of chunks as resource zones, signalling to players they'll be reset, and 
allowing admins to easily identify them to be reset.

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

- **Marked Zones**: Players can see them in-game, and are notified on entry. Admins can export them.
- **MCA Selector CSV Export**: Resource zones can be exported by an admin as a CSV (written as a 
  file on the hosting server), ready to be imported into MCA Selector.

### Usage

Admins add chunks to resource zones with `/resourcezones add`, and players can turn on particles
to see them with `/resourcezones particles`.

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

| Setting            | Default          | Description                                                           |
|--------------------|------------------|-----------------------------------------------------------------------|
| `particles.colour` | `gold`           | The particles' colour, as a colour name (e.g. `gold`), or hex.        |
| `particles.radius` | `16`             | How many blocks around each player (horizontally) zones are outlined. |
| `export.folder`    | `resource-zones` | The folder zones are exported to, inside the server's folder.         |

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

### Statistics

TODO

[1]: https://github.com/Querz/mcaselector
