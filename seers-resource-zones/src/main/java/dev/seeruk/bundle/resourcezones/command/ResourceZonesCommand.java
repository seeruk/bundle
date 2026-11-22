package dev.seeruk.bundle.resourcezones.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.seeruk.bundle.core.command.CommandExecutors;
import dev.seeruk.bundle.core.command.NamespacedCommands;
import dev.seeruk.bundle.resourcezones.ResourceZones;
import dev.seeruk.bundle.resourcezones.SeersResourceZones;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * {@code /resourcezones}: commands for managing resource zones and their chunks.
 */
public final class ResourceZonesCommand {
    static final String NAME = "resourcezones";

    private ResourceZonesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NamespacedCommands.register(
            dispatcher, SeersResourceZones.MOD_ID, NAME, name -> Commands.literal(name)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(chunkCommand("add", ResourceZonesCommand::add))
                .then(chunkCommand("remove", ResourceZonesCommand::remove))
        );
    }

    /**
     * Creates a subcommand that optionally accepts a column position argument, and acts on the
     * chunk at that position.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> chunkCommand(String name, ChunkAction action) {
        return Commands.literal(name)
            .executes(CommandExecutors.logged(context -> {
                final var source = context.getSource();
                return action.run(source, ChunkPos.containing(BlockPos.containing(source.getPosition())));
            }))
            .then(Commands.argument("pos", ColumnPosArgument.columnPos())
                .executes(CommandExecutors.logged(context ->
                    action.run(context.getSource(), ColumnPosArgument.getColumnPos(context, "pos").toChunkPos()))));
    }

    @FunctionalInterface
    private interface ChunkAction {
        int run(CommandSourceStack source, ChunkPos pos);
    }

    private static int add(CommandSourceStack source, ChunkPos pos) {
        if (!ResourceZones.add(source.getServer(), source.getLevel().dimension(), pos)) {
            source.sendFailure(TextParsers.parseAsTrusted(
                "<yellow>Chunk %s is already in a resource zone</yellow>".formatted(describe(pos))
            ));
            return 0;
        }

        final var message = TextParsers.parseAsTrusted(
            "<green>Added chunk %s to resource zones</green>".formatted(describe(pos))
        );
        source.sendSuccess(() -> message, true);
        return 1;
    }

    private static int remove(CommandSourceStack source, ChunkPos pos) {
        if (!ResourceZones.remove(source.getServer(), source.getLevel().dimension(), pos)) {
            source.sendFailure(TextParsers.parseAsTrusted(
                "<yellow>Chunk %s isn't in a resource zone</yellow>".formatted(describe(pos))
            ));
            return 0;
        }

        final var message = TextParsers.parseAsTrusted(
            "<green>Removed chunk %s from resource zones</green>".formatted(describe(pos))
        );
        source.sendSuccess(() -> message, true);
        return 1;
    }

    private static String describe(ChunkPos pos) {
        return "[%d, %d]".formatted(pos.x(), pos.z());
    }
}
