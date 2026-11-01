package dev.seeruk.bundle.disposal.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.command.NamespacedCommands;
import dev.seeruk.bundle.disposal.DisposalMenu;
import dev.seeruk.bundle.disposal.SeersDisposal;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;

/**
 * {@code /disposal}: opens a disposal inventory for the calling player
 */
public final class DisposalCommand {
    private static final Component TITLE = Component.literal("Disposal");

    private DisposalCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NamespacedCommands.register(dispatcher, SeersDisposal.MOD_ID, "disposal",
            name -> Commands.literal(name).executes(DisposalCommand::open)
        );
    }

    private static int open(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final var player = context.getSource().getPlayerOrException();
        if (player.isSpectator()) {
            context.getSource().sendFailure(Component.literal("You can't use Disposal while spectating"));
            return 0;
        }

        player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new DisposalMenu(id, inventory), TITLE));
        return 1;
    }
}
