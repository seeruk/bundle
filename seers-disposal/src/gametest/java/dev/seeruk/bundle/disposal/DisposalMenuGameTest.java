package dev.seeruk.bundle.disposal;

import dev.seeruk.bundle.disposal.api.DisposalEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class DisposalMenuGameTest {
    // Since listeners can't be removed, we keep track of events for all players in one list and
    // filter it in each test. It's safe to do though, because tests are run on the server thread
    // and have their own players; though, test players may share a name.
    private static final List<Disposed> EVENTS = new ArrayList<>();

    static {
        DisposalEvents.DISPOSED.register((player, items) -> EVENTS.add(new Disposed(player, items)));
    }

    @SuppressWarnings("removal")
    @GameTest
    public void closingDestroysItems(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var container = new SimpleContainer(DisposalMenu.ROWS * DisposalMenu.COLUMNS);
        container.setItem(0, new ItemStack(Items.DIRT, 32));
        container.setItem(1, new ItemStack(Items.DIAMOND, 27));

        open(player, container);
        player.closeContainer();

        helper.assertTrue(container.isEmpty(), "Items weren't destroyed");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void closingDoesNotReturnOrDropItems(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var container = new SimpleContainer(DisposalMenu.ROWS * DisposalMenu.COLUMNS);
        container.setItem(0, new ItemStack(Items.DIRT, 32));

        open(player, container);
        player.closeContainer();

        final var dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8));

        helper.assertTrue(player.getInventory().isEmpty(), "Items were returned to the player");
        helper.assertTrue(dropped.isEmpty(), "Items were dropped");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void closingWithItemsFiresEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var container = new SimpleContainer(DisposalMenu.ROWS * DisposalMenu.COLUMNS);
        container.setItem(0, new ItemStack(Items.DIRT, 32));
        container.setItem(1, new ItemStack(Items.DIAMOND, 27));

        open(player, container);
        player.closeContainer();

        final var events = eventsFor(player);
        helper.assertValueEqual(events.size(), 1, "number of events");

        final var items = events.getFirst().items();
        helper.assertValueEqual(items.size(), 2, "number of items");
        helper.assertTrue(items.get(0).is(Items.DIRT) && items.get(0).getCount() == 32, "Expected 32 dirt");
        helper.assertTrue(items.get(1).is(Items.DIAMOND) && items.get(1).getCount() == 27, "Expected 27 diamonds");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void closingWithoutItemsDoesNotFireEvent(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();

        open(player, new SimpleContainer(DisposalMenu.ROWS * DisposalMenu.COLUMNS));
        player.closeContainer();

        helper.assertTrue(eventsFor(player).isEmpty(), "Event fired without items");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void disposedEventSkipsEmptyStacks(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var container = new SimpleContainer(DisposalMenu.ROWS * DisposalMenu.COLUMNS);
        container.setItem(3, new ItemStack(Items.DIRT, 1));
        container.setItem(40, new ItemStack(Items.STONE, 2));

        open(player, container);
        player.closeContainer();

        final var events = eventsFor(player);
        final var items = events.getFirst().items();

        helper.assertValueEqual(events.size(), 1, "number of events");
        helper.assertValueEqual(items.size(), 2, "number of items");
        helper.assertTrue(items.get(0).is(Items.DIRT) && items.get(0).getCount() == 1, "Expected 1 dirt");
        helper.assertTrue(items.get(1).is(Items.STONE) && items.get(1).getCount() == 2, "Expected 2 stone");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void disposedEventReceivesCopies(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var container = new SimpleContainer(DisposalMenu.ROWS * DisposalMenu.COLUMNS);
        final var original = new ItemStack(Items.DIRT, 16);
        container.setItem(0, original);

        open(player, container);
        player.closeContainer();

        final var events = eventsFor(player);
        helper.assertValueEqual(events.size(), 1, "number of events");
        helper.assertTrue(events.getFirst().items().getFirst() != original, "Event received original stack");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void carriedItemIsNotDisposed(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();

        open(player, new SimpleContainer(DisposalMenu.ROWS * DisposalMenu.COLUMNS));
        player.containerMenu.setCarried(new ItemStack(Items.DIAMOND, 1));
        player.closeContainer();

        helper.assertTrue(eventsFor(player).isEmpty(), "Carried item was disposed");
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 1, "Carried item wasn't returned");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    @GameTest
    public void inventoryIsCorrectSize(GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        final var menu = new DisposalMenu(0, player.getInventory());

        helper.assertValueEqual(menu.getType(), MenuType.GENERIC_9x6, "menu type");
        helper.assertValueEqual(menu.getRowCount(), 6, "rows");
        helper.assertValueEqual(menu.getContainer().getContainerSize(), 54, "container size");
        helper.succeed();
    }

    private record Disposed(Player player, List<ItemStack> items) {}

    private static void open(ServerPlayer player, SimpleContainer container) {
        player.openMenu(new SimpleMenuProvider(
            (id, inventory, owner) -> new DisposalMenu(id, inventory, container),
            Component.literal("Disposal")
        ));
    }

    private static List<Disposed> eventsFor(Player player) {
        return EVENTS.stream().filter(event -> event.player() == player).toList();
    }
}
