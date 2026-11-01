# Seer's Disposal

Provides a disposal inventory for quick and easy item destruction, without littering...


## Features

* **Disposal Inventory**: A double-chest sized inventory, openable on command.
* **Optional Client Mod**: Adds a quick-access key binding for opening the disposal inventory.
* **Event Hooks**: Fires an event when items are disposed, allowing other mods to react.


## Usage

You can always open the disposal inventory by running the `/disposal` command, this only requires
the mod server-side. If you also have the client-side mod installed, you can set a key binding to
open the disposal inventory without needing to type the command (it just sends the command for you).

**Note:** Vanilla clients can still connect without the client-side mod.

**Warning:** Items are genuinely permanently destroyed when the inventory is closed. This includes
things like putting items in and then logging out, being disconnected, dying, or the server 
crashing. Basically, do not put anything in it that you might want to keep.


### Commands

| Command     | Description                   |
|-------------|-------------------------------|
| `/disposal` | Opens the disposal inventory. |


### Key Bindings (Client-side Only)

| Key Binding     | Category  | Description                   |
|-----------------|-----------|-------------------------------|
| `Open Disposal` | INVENTORY | Opens the disposal inventory. |


### Events

#### DISPOSED

Fired when items are disposed of via the disposal inventory. Only fires if at least one item was 
actually destroyed (i.e. not just fired every time the inventory is closed).

```
dev.seeruk.disposal.api.DisposalEvents.DISPOSED
```

**Callback signature:**

```java
void onDisposed(ServerPlayer player, List<ItemStack> items);
```

| Parameter | Description                   |
|-----------|-------------------------------|
| `player`  | The player who disposed items |
| `items`   | The items that were disposed  |


### Statistics

| Scoreboard Objective            | Description                                        |
|---------------------------------|----------------------------------------------------|
| `seers-disposal.items_disposed` | The total number of items a player has disposed of |

Objectives can be removed while the server is running, but will be recreated on server restart. 
Removing an objective will reset it for all players.
