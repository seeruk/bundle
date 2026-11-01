package dev.seeruk.bundle.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.function.Function;

public final class NamespacedCommands {
    private NamespacedCommands() {}

    /**
     * Registers the given command tree under both the given name and a namespaced variant,
     * prefixing the name with the given mod ID. Avoids scenarios where a mod can steal a command
     * by registering it under the same name accidentally.
     *
     * <p>The command tree is built twice instead of using the Brigadier redirect feature, because
     * redirects don't use the root command's own {@code executes}.
     */
    public static void register(
        CommandDispatcher<CommandSourceStack> dispatcher,
        String modId,
        String name,
        Function<String, LiteralArgumentBuilder<CommandSourceStack>> tree
    ) {
        dispatcher.register(tree.apply(name));
        dispatcher.register(tree.apply(modId + ":" + name));
    }
}
