# Seer's PropSwap

Debug-stick-like behaviours, but on regular tools! This is a datapack-driven mod that allows various
types of blocks to have various properties / block states altered while using appropriate tools. 
What does that mean? You can do things like unlink a fence from a wall, or glass panes from a block 
behind them, and more!

## Features

- **PropSwapping**: "Use" a block with the right tool to change how it's shaped or connected.
- **Togglable**: PropSwap is not on all of the time, run a command to activate it for safer use.
- **Locked Changes**: Changed blocks stay that way, even when blocks around them change.
- **Datapack-friendly Tools**: Which tools work on which blocks is driven by block tags.
- **Event Hooks**: Fires an event when a block is PropSwapped, allowing other mods to react.
- **Optional Client Mod**: Stops your client briefly showing what Vanilla would do instead.

## Usage

This mod is server-side, and optionally client-side.

Run `/propswap` to turn PropSwapping on, then use (right-click) a block while holding the tool that
would break it. Crouching and using can also modify some blocks in a slightly different way. As 
properties are changed, an indication of what changed will show up above the action bar, similar to
the real debug stick.

Changed blocks are locked in place, so they don't get undone as soon as something next to them
updates (the debug stick's biggest annoyance...). The lock is cleared when the block is broken or
replaced.

If you don't use PropSwap for 5 minutes, it'll turn itself off. When you log into a server it
will be disabled to begin with, and won't remember if you previously had it enabled. This is 
primarily to prevent accidental changes to blocks!

The optional client-side mod ensures that your client doesn't show flickering blocks or do things
like play sounds while prop swapping. The client-side mod uses the same logic as the server-side mod
to determine what to ignore while PropSwap is enabled. It also knows which blocks are locked, so
placing blocks next to them doesn't make them flicker either.

**Note:** Vanilla clients can still connect, and use PropSwap, without the client-side mod.

### Commands

| Command     | Description                            |
|-------------|----------------------------------------|
| `/propswap` | Toggles PropSwapping on or off for you |

### Supported Blocks

| Block                        | Click                                                      | Crouch                                   |
|------------------------------|------------------------------------------------------------|------------------------------------------|
| Fences, glass panes, bars    | Toggles the connection on the clicked side                 | Toggles every side                       |
| Walls                        | Cycles the clicked side (none, low, tall)                  | Toggles the post, or every side from top |
| Stairs                       | Cycles the shape, corners included                         | Cycles the shape backwards               |
| Mushroom blocks and stems    | Toggles the clicked face (pores or skin)                   | -                                        |
| Pillars (logs, basalt, etc.) | Cycles the axis                                            | Cycles the axis backwards                |
| All furnaces and smokers     | Toggles whether it looks lit                               | -                                        |
| Barrels                      | Faces the clicked side, or flips it                        | Toggles whether it's forced open         |
| Beds                         | Splits the bed, then flips the clicked half (head or foot) | -                                        |

Clicking the top or bottom of a fence, pane, bar or wall changes the side you're facing instead. For
walls, crouching on a side toggles the post, and crouch-clicking on the top or bottom toggles every 
side. A wall always keeps its post if it would otherwise have nothing left, as an empty wall is 
invisible and impossible for a player to then remove.

Splitting a bed allows each half to be broken on its own. Splitting costs a bed of the same kind, 
since you've basically got two beds now. Each half drops a bed when it's broken, so you always get 
back what you put in. Flipping halves after that is free, and creative players don't have to pay to 
split.

### Tools

Tools match blocks using PropSwap's own block tags (e.g. `seers-propswap:tool/axe`), which just
include the Vanilla `minecraft:mineable/*` tags by default. So wood takes an axe, stone takes a
pickaxe, and datapacks that add blocks to those tags (like making glass a pickaxe block) just work.
PropSwap's tags can also be changed on their own, without touching Vanilla behaviour.

Some supported blocks have no tool in Vanilla, so PropSwap's tags give them one. Beds use an axe
(straw beds already use a hoe in Vanilla, so they use a hoe here too), and glass panes use a pickaxe
(via the `c:glass_panes` tag, so modded panes are included too). Any other block with no tool won't
do anything until a datapack adds it to one of PropSwap's tags.

### Events

#### PropSwapEvents.SWAPPED

Fired when a player PropSwaps a block, just after it's changed. Not fired when nothing changed.

```
dev.seeruk.bundle.propswap.api.PropSwapEvents.SWAPPED
```

**Callback signature:**

```java
void onSwapped(Player player, Level level, BlockPos pos, BlockState original, BlockState swapped);
```

| Parameter  | Description                           |
|------------|---------------------------------------|
| `player`   | The player that PropSwapped the block |
| `level`    | The level the block is in             |
| `pos`      | The position of the block             |
| `original` | The block's state before the swap     |
| `swapped`  | The block's state after the swap      |

### Statistics

| Scoreboard Objective            | Description                                         |
|---------------------------------|-----------------------------------------------------|
| `seers_propswap.blocks_swapped` | The total number of blocks a player has PropSwapped |

Objectives can be removed while the server is running, but will be recreated on server restart.
Removing an objective will reset it for all players.
