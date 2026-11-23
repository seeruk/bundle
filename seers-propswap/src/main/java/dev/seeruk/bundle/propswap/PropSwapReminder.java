package dev.seeruk.bundle.propswap;

import dev.seeruk.bundle.core.actionbar.ActionBarMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import java.util.Optional;

/**
 * A "PropSwap enabled" reminder, kept in the action bar (see {@link ActionBarMessages}) while a
 * player has PropSwap on and is holding a tool it works with.
 */
public final class PropSwapReminder {
    private static final Component MESSAGE = Component.literal("PropSwap enabled").withStyle(ChatFormatting.GRAY);

    private PropSwapReminder() {}

    public static Optional<Component> message(ServerPlayer player) {
        return PropSwapMode.isEnabled(player) && holdingTool(player) ? Optional.of(MESSAGE) : Optional.empty();
    }

    private static boolean holdingTool(ServerPlayer player) {
        return PropSwapTool.isTool(player.getItemInHand(InteractionHand.MAIN_HAND))
            || PropSwapTool.isTool(player.getItemInHand(InteractionHand.OFF_HAND));
    }
}
