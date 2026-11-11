package dev.seeruk.bundle.itemtext.command;

import com.mojang.brigadier.context.CommandContext;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public abstract class ItemTextCommand {
    protected static Optional<ItemStack> mainHandItem(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        if (player.isSpectator()) {
            context.getSource().sendFailure(TextParsers.parseAsTrusted(
                "<red>You can't modify this item while spectating!</red>"
            ));
            return Optional.empty();
        }

        final var stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            context.getSource().sendFailure(TextParsers.parseAsTrusted(
                "<red>Hold an item in your main hand for it to be modified</red>"
            ));
            return Optional.empty();
        }

        return Optional.of(stack);
    }
}
