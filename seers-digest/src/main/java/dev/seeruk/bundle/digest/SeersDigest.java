package dev.seeruk.bundle.digest;

import dev.seeruk.bundle.core.api.PlayerEvents;
import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.core.stats.AllPlayerStats;
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

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        final var config = ModConfig.load(MOD_ID, DigestConfig.CODEC);
        // Says what time it is in the configured time zone, which the schedule and night hours are in
        final var clock = Clock.system(config.timezone());

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
            DigestCommand.register(dispatcher, config, clock));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            config.groups().forEach(group -> AllPlayerStats.unknownNames(server, group.stats()).forEach(name ->
                LOGGER.warn("Group '{}' uses '{}', which isn't a stat (yet), so it's counted as 0", group.id(), name)));
            IssueSchedule.started(server);
            DigestStats.ensureObjectives(server);
            Moments.seed(server);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            IssueSchedule.tick(server, config, clock);
            if (server.getTickCount() % SharedConstants.TICKS_PER_SECOND == 0) {
                DigestStats.recordNightSecond(server, config.nightHours(), clock);
            }
        });

        // Handle events Digest tracks on it's own
        PlayerEvents.ADVANCEMENT_MESSAGE.register((player, advancement, message) -> Moments.earned(player, advancement));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
            DigestStats.recordDimensionChange(player, destination.dimension()));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                Moments.died(player, source);
            }
        });

        LOGGER.info("Initialized");
    }
}
