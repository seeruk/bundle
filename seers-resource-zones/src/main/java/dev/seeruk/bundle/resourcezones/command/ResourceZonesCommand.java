package dev.seeruk.bundle.resourcezones.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.seeruk.bundle.core.command.CommandExecutors;
import dev.seeruk.bundle.core.command.NamespacedCommands;
import dev.seeruk.bundle.resourcezones.ResourceZones;
import dev.seeruk.bundle.resourcezones.SeersResourceZones;
import dev.seeruk.bundle.resourcezones.particles.ParticleIntensity;
import dev.seeruk.bundle.resourcezones.particles.ParticleSettings;
import dev.seeruk.bundle.resourcezones.particles.ParticleStyle;
import dev.seeruk.bundle.resourcezones.particles.ZoneParticles;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * {@code /resourcezones}: all resource-zone related commands. For admins, managing resource zones
 * and their chunks; and for all players, to manage particle settings.
 */
public final class ResourceZonesCommand {
    static final String NAME = "resourcezones";

    private static final Predicate<CommandSourceStack> ADMIN = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);

    private ResourceZonesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NamespacedCommands.register(
            dispatcher, SeersResourceZones.MOD_ID, NAME, name -> Commands.literal(name)
                .then(chunkCommand("add", ResourceZonesCommand::add).requires(ADMIN))
                .then(chunkCommand("remove", ResourceZonesCommand::remove).requires(ADMIN))
                .then(particlesCommand())
        );
    }

    /**
     * Creates a subcommand that acts on the chunk at the given column position, or without one, the
     * chunk the entity running it is in. Without one, it needs an entity, as the console runs
     * commands from the world spawn.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> chunkCommand(String name, ChunkAction action) {
        return Commands.literal(name)
            .executes(CommandExecutors.logged(context -> {
                final var source = context.getSource();
                return action.run(source, source.getEntityOrException().chunkPosition());
            }))
            .then(Commands.argument("pos", ColumnPosArgument.columnPos())
                .executes(CommandExecutors.logged(context ->
                    action.run(context.getSource(), ColumnPosArgument.getColumnPos(context, "pos").toChunkPos()))));
    }

    @FunctionalInterface
    private interface ChunkAction {
        int run(CommandSourceStack source, ChunkPos pos);
    }

    /**
     * The {@code particles} subcommand, which shows the player's particle settings, with a
     * subcommand for each intensity, and a {@code style} subcommand for each style.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> particlesCommand() {
        final var particles = Commands.literal("particles")
            .executes(CommandExecutors.logged(context -> describeParticles(context.getSource())));

        for (final var intensity : ParticleIntensity.values()) {
            particles.then(Commands.literal(intensity.getSerializedName())
                .executes(CommandExecutors.logged(context -> changeParticles(
                    context.getSource(),
                    settings -> settings.withIntensity(intensity),
                    intensity == ParticleIntensity.OFF
                        ? "Resource zone particles turned off"
                        : "Resource zone particles set to " + intensity.getSerializedName()
                ))));
        }

        final var style = Commands.literal("style");
        for (final var value : ParticleStyle.values()) {
            style.then(Commands.literal(value.getSerializedName())
                .executes(CommandExecutors.logged(context -> changeParticles(
                    context.getSource(),
                    settings -> settings.withStyle(value),
                    "Resource zone particles set to the " + value.getSerializedName() + " style"
                ))));
        }

        return particles.then(style);
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

    private static int describeParticles(CommandSourceStack source) throws CommandSyntaxException {
        final var settings = ZoneParticles.settings(source.getPlayerOrException());
        final var message = Component.literal("Resource zone particles: %s, %s style".formatted(
            settings.intensity().getSerializedName(),
            settings.style().getSerializedName()
        ));

        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int changeParticles(
        CommandSourceStack source,
        UnaryOperator<ParticleSettings> update,
        String message
    ) throws CommandSyntaxException {
        ZoneParticles.updateSettings(source.getPlayerOrException(), update);

        final var changed = TextParsers.parseAsTrusted("<green>" + message + "</green>");
        source.sendSuccess(() -> changed, false);
        return 1;
    }

    private static String describe(ChunkPos pos) {
        return "[%d, %d]".formatted(pos.x(), pos.z());
    }
}
