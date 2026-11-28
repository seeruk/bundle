package dev.seeruk.bundle.digest;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import dev.seeruk.bundle.core.config.DistConfigs;
import dev.seeruk.bundle.core.config.ModConfig;
import dev.seeruk.bundle.core.stats.StatUnit;
import dev.seeruk.bundle.core.time.Schedule;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DigestConfigTest {
    private static final Map<String, Object> SCHEDULE = Map.of("every", "week", "on", "sunday", "at", "18:00");
    private static final Map<String, Object> NIGHT = Map.of("from", "22:00", "to", "04:00");

    // A config with everything it needs, with the given settings replacing those
    private static DataResult<DigestConfig> parse(Map<String, Object> settings) {
        final var config = new HashMap<String, Object>(Map.of(
            "schedule", SCHEDULE,
            "night_hours", NIGHT,
            "groups", List.of()
        ));
        config.putAll(settings);
        return DigestConfig.CODEC.parse(JavaOps.INSTANCE, config);
    }

    private static Map<String, Object> group(String id) {
        return Map.of("id", id, "stats", List.of("minecraft.mined:*"));
    }

    @Test
    void readsTheShippedConfig() {
        assertDoesNotThrow(() -> DistConfigs.read(SeersDigest.MOD_ID, ModConfig.CONFIG, DigestConfig.CODEC));
    }

    @Test
    void readsEverySetting() {
        final var config = parse(Map.of(
            "timezone", "Europe/London",
            "groups", List.of(group("miner"))
        )).getOrThrow();

        assertEquals(ZoneId.of("Europe/London"), config.timezone());
        assertEquals(Schedule.weekly(DayOfWeek.SUNDAY, LocalTime.of(18, 0)), config.schedule());
        assertEquals(new NightHours(LocalTime.of(22, 0), LocalTime.of(4, 0)), config.nightHours());
        assertEquals(List.of("miner"), config.groups().stream().map(DigestConfig.StatGroup::id).toList());
    }

    @Test
    void defaultsToTheServersTimeZone() {
        assertEquals(ZoneId.systemDefault(), parse(Map.of()).getOrThrow().timezone());
    }

    @Test
    void readsAGroupsUnit() {
        final var group = Map.of("id", "explorer", "stats", List.of("minecraft.custom:*"), "unit", "distance");
        final var config = parse(Map.of("groups", List.of(group))).getOrThrow();

        assertEquals(StatUnit.DISTANCE, config.groups().getFirst().unit());
    }

    @Test
    void defaultsGroupsToCountingAsTheyAre() {
        final var config = parse(Map.of("groups", List.of(group("miner")))).getOrThrow();

        assertEquals(StatUnit.COUNT, config.groups().getFirst().unit());
    }

    @Test
    void refusesGroupIdsUsedMoreThanOnce() {
        assertTrue(parse(Map.of("groups", List.of(group("miner"), group("miner")))).isError());
    }

    @Test
    void refusesGroupIdsTemplatesCantUse() {
        assertTrue(parse(Map.of("groups", List.of(group("miner.stone")))).isError());
        assertTrue(parse(Map.of("groups", List.of(group("miner-stone")))).isError());
    }

    @Test
    void refusesTimeZonesThatAreNotTimeZones() {
        assertTrue(parse(Map.of("timezone", "Nowhere/Special")).isError());
    }

    @Test
    void refusesNightHoursThatStartWhenTheyEnd() {
        assertTrue(parse(Map.of("night_hours", Map.of("from", "22:00", "to", "22:00"))).isError());
    }

    @Test
    void refusesGroupIdsTheBookUses() {
        // Templates use issue for the issue's own values (e.g. ${issue.number}), and Spotlight uses
        // default for groups without their own wording
        assertTrue(parse(Map.of("groups", List.of(group("issue")))).isError());
        assertTrue(parse(Map.of("groups", List.of(group("default")))).isError());
    }
}
