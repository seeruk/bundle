package dev.seeruk.bundle.itemtext;

import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Finds the item a player wants to edit the text of.
 */
public final class EditableItem {
    private EditableItem() {}

    /**
     * The item in the player's main hand, if they're allowed to edit it.
     *
     * @param player  The player editing the item.
     * @param refusal Given the reason, to tell the player, if they can't edit their item.
     */
    public static Optional<ItemStack> inMainHand(ServerPlayer player, Consumer<Component> refusal) {
        if (player.isSpectator()) {
            refusal.accept(TextParsers.parseAsTrusted("<red>You can't modify this item while spectating!</red>"));
            return Optional.empty();
        }

        final var stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            refusal.accept(TextParsers.parseAsTrusted("<red>Hold an item in your main hand for it to be modified</red>"));
            return Optional.empty();
        }

        return Optional.of(stack);
    }
}
