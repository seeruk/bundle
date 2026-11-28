package dev.seeruk.bundle.digest;

import dev.seeruk.bundle.core.api.PlayerEvents;
import dev.seeruk.bundle.core.config.ConfigException;
import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.core.stats.AllPlayerStats;
import dev.seeruk.bundle.digest.api.DigestEvents;
import dev.seeruk.bundle.digest.book.BookConfig;
import dev.seeruk.bundle.digest.book.DigestBooks;
import dev.seeruk.bundle.digest.command.DigestCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;

public class SeersDigest implements ModInitializer {
    public static final String MOD_ID = "seers-digest";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // What the book says, in book.yml
    private static final String BOOK = "book";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        final var config = ModConfig.load(MOD_ID, DigestConfig.CODEC);
        final var book = ModConfig.load(MOD_ID, BOOK, BookConfig.CODEC);
        final var unknownGroups = book.unknownGroups(config.groups().stream().map(DigestConfig.StatGroup::id).toList());
        if (!unknownGroups.isEmpty()) {
            throw new ConfigException("The book names groups that aren't in the config: " + String.join(", ", unknownGroups));
        }
        final var clock = Clock.system(config.timezone());

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            DigestCommand.register(dispatcher, config, book, clock));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            config.groups().forEach(group -> AllPlayerStats.unknownNames(server, group.stats()).forEach(name ->
                LOGGER.warn("Group '{}' uses '{}', which isn't a stat (yet), so it's counted as 0", group.id(), name)));
            IssueSchedule.forgetOldPeriods(server);
            DigestStats.ensureObjectives(server);
            Moments.seed(server);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Once a second, so issues are published within a second of the schedule. It's checked
            // from ticks, rather than on start-up, so an issue due while the server was stopped is
            // only published once every mod has started, and can listen for it.
            if (server.getTickCount() % SharedConstants.TICKS_PER_SECOND == 0) {
                IssueSchedule.publishIfDue(server, config, clock);
                DigestStats.recordNightSecond(server, config.nightHours(), clock);
            }
        });

        DigestEvents.PUBLISHED.register((server, issue) -> DigestBooks.warnIfTooLong(server, config, book, issue));

        // Handle events Digest tracks on its own
        PlayerEvents.ADVANCEMENT_MESSAGE.register((player, advancement, message) ->
            Moments.recordAdvancement(player, advancement, clock));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
            DigestStats.recordDimensionChange(player, destination.dimension()));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                Moments.recordDeath(player, source, clock);
            }
        });

        LOGGER.info("Initialized");
    }
}
