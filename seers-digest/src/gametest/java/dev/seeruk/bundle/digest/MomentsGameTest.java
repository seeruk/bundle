package dev.seeruk.bundle.digest;

import dev.seeruk.bundle.digest.api.Issue;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.SharedConstants;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class MomentsGameTest {
    // Moments and firsts are shared by every test on the server, so each test only checks its own
    // players, takes its moments in the same tick it records them (so no other test can take them
    // first), and uses a cause of death or advancement no other test uses, as firsts are only ever
    // firsts once.

    private static void kill(GameTestHelper helper, ServerPlayer player, DamageSource source) {
        player.hurtServer(helper.getLevel(), source, Float.MAX_VALUE);
    }

    private static void award(GameTestHelper helper, ServerPlayer player, String path) {
        final AdvancementHolder advancement = Objects.requireNonNull(
            helper.getLevel().getServer().getAdvancements().get(Identifier.withDefaultNamespace(path)),
            path
        );
        advancement.value().criteria().keySet().forEach(criterion -> player.getAdvancements().award(advancement, criterion));
    }

    private static List<Issue.Death> deathsOf(Issue.Moments moments, ServerPlayer player) {
        return moments.deaths().stream().filter(death -> death.player().equals(player.getUUID())).toList();
    }

    private static List<String> firstsOf(Issue.Moments moments, ServerPlayer player) {
        return moments.firsts().stream().filter(first -> first.player().equals(player.getUUID())).map(Issue.First::what).toList();
    }

    @GameTest
    public void deathsAreRecordedWithTheirCauseAndKiller(GameTestHelper helper) {
        final var player = player(helper);
        final var zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);

        kill(helper, player, helper.getLevel().damageSources().mobAttack(zombie));
        final var moments = Moments.take(helper.getLevel().getServer());

        helper.assertValueEqual(
            deathsOf(moments, player).stream().map(death -> List.of(death.cause(), death.killer())).toList(),
            List.of(List.of("minecraft:mob_attack", Optional.of("minecraft:zombie"))),
            "deaths"
        );
        // Nobody else in these tests is killed by a mob, or a zombie
        helper.assertValueEqual(
            firstsOf(moments, player),
            List.of("death:minecraft:mob_attack", "killer:minecraft:zombie"),
            "firsts"
        );
        helper.succeed();
    }

    @GameTest
    public void deathsAfterBeingHurtAreCreditedToWhoeverHurtThem(GameTestHelper helper) {
        final var player = player(helper);
        final var skeleton = EntityTypes.SKELETON.create(helper.getLevel(), EntitySpawnReason.COMMAND);

        // Knocked off a cliff by a skeleton, rather than killed by its arrow
        player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(skeleton), 1);
        kill(helper, player, helper.getLevel().damageSources().fall());
        final var moments = Moments.take(helper.getLevel().getServer());

        helper.assertValueEqual(
            deathsOf(moments, player).stream().map(Issue.Death::killer).toList(),
            List.of(Optional.of("minecraft:skeleton")),
            "killers"
        );
        helper.succeed();
    }

    @GameTest
    public void onlyTheFirstDeathOfACauseIsAFirst(GameTestHelper helper) {
        final var first = player(helper);
        final var second = player(helper);

        kill(helper, first, helper.getLevel().damageSources().lava());
        kill(helper, second, helper.getLevel().damageSources().lava());
        final var moments = Moments.take(helper.getLevel().getServer());

        helper.assertValueEqual(deathsOf(moments, second).size(), 1, "second's deaths");
        helper.assertValueEqual(firstsOf(moments, first), List.of("death:minecraft:lava"), "first's firsts");
        helper.assertValueEqual(firstsOf(moments, second), List.of(), "second's firsts");
        helper.succeed();
    }

    @GameTest
    public void onlyTheFirstToEarnAnAdvancementIsAFirst(GameTestHelper helper) {
        final var first = player(helper);
        final var second = player(helper);

        award(helper, first, "story/mine_stone");
        award(helper, second, "story/mine_stone");
        final var moments = Moments.take(helper.getLevel().getServer());

        helper.assertValueEqual(
            moments.advancements().stream()
                .filter(earned -> earned.player().equals(second.getUUID()))
                .map(Issue.Earned::advancement)
                .toList(),
            List.of("minecraft:story/mine_stone"),
            "second's advancements"
        );
        helper.assertValueEqual(firstsOf(moments, first), List.of("advancement:minecraft:story/mine_stone"), "first's firsts");
        helper.assertValueEqual(firstsOf(moments, second), List.of(), "second's firsts");
        helper.succeed();
    }

    @GameTest
    public void takingMomentsForgetsThem(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper);

        kill(helper, player, helper.getLevel().damageSources().drown());

        helper.assertValueEqual(deathsOf(Moments.take(server), player).size(), 1, "deaths taken");
        helper.assertValueEqual(deathsOf(Moments.take(server), player), List.of(), "deaths taken again");
        helper.succeed();
    }

    @GameTest
    public void existingMomentsAreReadFromPlayersSavedFiles(GameTestHelper helper) throws IOException {
        final var server = helper.getLevel().getServer();
        // An offline player who played before Digest was added
        final var id = UUID.randomUUID();
        final var advancements = server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(id + ".json");
        final var stats = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(id + ".json");

        try {
            Files.createDirectories(advancements.getParent());
            Files.writeString(advancements, """
                {
                    "minecraft:nether/find_fortress": {"criteria": {"fortress": "2026-01-01 00:00:00 +0000"}, "done": true},
                    "minecraft:nether/explore_nether": {"criteria": {}, "done": false},
                    "minecraft:recipes/decorations/crafting_table": {"criteria": {}, "done": true},
                    "DataVersion": 1
                }
                """);
            Files.createDirectories(stats.getParent());
            Files.writeString(stats, """
                {"stats": {"minecraft:killed_by": {"minecraft:enderman": 1}}, "DataVersion": %d}
                """.formatted(SharedConstants.getCurrentVersion().dataVersion().version()));

            final var existing = Moments.existing(server);

            helper.assertTrue(existing.contains("advancement:minecraft:nether/find_fortress"), "Earned advancement should have happened");
            helper.assertTrue(!existing.contains("advancement:minecraft:nether/explore_nether"), "Unfinished advancement shouldn't have happened");
            // Recipes are advancements the game doesn't announce, so they're never firsts
            helper.assertTrue(
                !existing.contains("advancement:minecraft:recipes/decorations/crafting_table"),
                "Unannounced advancement shouldn't count"
            );
            helper.assertTrue(existing.contains("killer:minecraft:enderman"), "Killer from stats should have happened");
        } finally {
            Files.deleteIfExists(advancements);
            Files.deleteIfExists(stats);
        }

        helper.succeed();
    }
}
