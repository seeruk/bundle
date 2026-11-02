package dev.seeruk.bundle.disposal;

import dev.seeruk.bundle.disposal.api.DisposalEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class DisposalMenu extends ChestMenu {
    public static final int ROWS = 6;
    public static final int COLUMNS = 9;

    private final SimpleContainer container;

    public DisposalMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(ROWS * COLUMNS));
    }

    public DisposalMenu(
        int containerId,
        Inventory inventory,
        SimpleContainer container
    ) {
        super(MenuType.GENERIC_9x6, containerId, inventory, container, ROWS);
        this.container = container;
    }

    private static void playSfx(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        serverPlayer.connection.send(new ClientboundSoundPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.LAVA_EXTINGUISH),
            SoundSource.BLOCKS,
            player.getX(),
            player.getY(),
            player.getZ(),
            0.5f,
            1f,
            player.getRandom().nextLong()
        ));
    }

    @Override
    public void removed(@NonNull Player player) {
        super.removed(player);

        final var items = container
            .removeAllItems()
            .stream()
            .filter(stack -> !stack.isEmpty())
            .map(ItemStack::copy)
            .toList();

        if (items.isEmpty()) {
            return;
        }

        DisposalEvents.DISPOSED.invoker().onDisposed(player, items);

        final var count = items.stream().mapToInt(ItemStack::getCount).sum();

        playSfx(player);
        player.sendOverlayMessage(Component.literal(disposedMessage(count)));
    }

    static String disposedMessage(int count) {
        return "Disposed of %d item%s".formatted(count, count == 1 ? "" : "s");
    }
}
