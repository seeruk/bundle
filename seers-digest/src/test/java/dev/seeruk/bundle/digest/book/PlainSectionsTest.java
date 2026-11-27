package dev.seeruk.bundle.digest.book;

import dev.seeruk.bundle.core.stats.StatSelection;
import dev.seeruk.bundle.core.stats.StatUnit;
import dev.seeruk.bundle.core.time.Schedule;
import dev.seeruk.bundle.digest.DigestConfig;
import dev.seeruk.bundle.digest.DigestConfig.StatGroup;
import dev.seeruk.bundle.digest.NightHours;
import dev.seeruk.bundle.digest.api.Issue;
import dev.seeruk.bundle.digest.api.Issue.GroupResult;
import dev.seeruk.bundle.digest.api.Issue.PlayerTotal;
import dev.seeruk.bundle.text.TextTemplate;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlainSectionsTest {
    private static final UUID SEER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID NEWBIE = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant AT = Instant.parse("2026-11-22T12:00:00Z");

    private static final DigestConfig CONFIG = new DigestConfig(
        ZoneOffset.UTC,
        Schedule.daily(LocalTime.of(18, 0)),
        new NightHours(LocalTime.MIDNIGHT, LocalTime.of(6, 0)),
        List.of(
            new StatGroup("explorer", StatSelection.of(List.of("walk")), StatUnit.DISTANCE),
            new StatGroup("angler", StatSelection.of(List.of("fish")), StatUnit.COUNT)
        ),
        new DigestConfig.Book("Digest", "Editors", "d MMM yyyy", TextTemplate.parse("${title}"))
    );

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static Issue issue(Map<String, GroupResult> groups, List<UUID> newPlayers, Issue.Moments moments) {
        return new Issue(1, AT, AT, groups, List.of(SEER, NEWBIE), newPlayers, moments);
    }

    // Each section's title and entries, as plain text
    private static Map<String, List<String>> sections(Issue issue) {
        final var sections = new LinkedHashMap<String, List<String>>();
        PlainSections.of(issue, CONFIG, id -> Component.literal(id.equals(SEER) ? "Seer" : "Newbie"))
            .forEach(section -> sections.put(
                section.title().getString(),
                section.entries().stream().map(Component::getString).toList()
            ));
        return sections;
    }

    @Test
    void showsTotalsInTheirGroupsUnit() {
        final var explorer = new GroupResult(150_000, List.of(new PlayerTotal(SEER, 150_000)));
        final var sections = sections(issue(Map.of("explorer", explorer), List.of(), Issue.Moments.EMPTY));

        assertEquals(List.of("2 active players", "0 new players", "explorer: 1,500 blocks"), sections.get("Headlines"));
        assertEquals(List.of("explorer\nSeer: 1,500 blocks"), sections.get("Awards"));
    }

    @Test
    void showsGroupsInTheConfigsOrder() {
        // Explorer comes first in the config, and the issue's groups are in no particular order
        final var groups = new LinkedHashMap<String, GroupResult>();
        groups.put("angler", new GroupResult(3, List.of(new PlayerTotal(NEWBIE, 2), new PlayerTotal(SEER, 1))));
        groups.put("explorer", new GroupResult(200, List.of(new PlayerTotal(SEER, 200))));
        final var sections = sections(issue(groups, List.of(), Issue.Moments.EMPTY));

        assertEquals(List.of("explorer\nSeer: 2 blocks", "angler\nNewbie: 2"), sections.get("Awards"));
    }

    @Test
    void leavesOutGroupsNobodyScoredIn() {
        final var groups = Map.of(
            "angler", new GroupResult(3, List.of(new PlayerTotal(NEWBIE, 3))),
            "explorer", new GroupResult(0, List.of())
        );
        final var sections = sections(issue(groups, List.of(), Issue.Moments.EMPTY));

        assertEquals(List.of("2 active players", "0 new players", "angler: 3"), sections.get("Headlines"));
        assertEquals(List.of("angler\nNewbie: 3"), sections.get("Awards"));
    }

    @Test
    void showsMomentsAndWelcomesNewPlayers() {
        final var moments = new Issue.Moments(
            List.of(new Issue.Death(SEER, "minecraft:mob_attack", Optional.of("minecraft:zombie"), AT)),
            List.of(new Issue.Earned(NEWBIE, "minecraft:story/mine_stone", AT)),
            List.of(new Issue.First(NEWBIE, "advancement:minecraft:story/mine_stone", AT))
        );
        final var sections = sections(issue(Map.of(), List.of(NEWBIE), moments));

        assertEquals(List.of("Seer died to minecraft:mob_attack, killed by minecraft:zombie"), sections.get("Deaths"));
        assertEquals(List.of("Newbie earned minecraft:story/mine_stone"), sections.get("Advancements"));
        assertEquals(List.of("Newbie was first: advancement:minecraft:story/mine_stone"), sections.get("Firsts"));
        assertEquals(List.of("Welcome, Newbie!"), sections.get("Welcome"));
    }

    @Test
    void leavesOutSectionsWithNothingToSay() {
        final var sections = sections(issue(Map.of(), List.of(), Issue.Moments.EMPTY));

        assertEquals(List.of("Headlines"), List.copyOf(sections.keySet()));
    }
}
