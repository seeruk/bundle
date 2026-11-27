package dev.seeruk.bundle.digest;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import dev.seeruk.bundle.core.time.Schedule;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DigestConfigTest {
    private static final Map<String, Object> SCHEDULE = Map.of("every", "week", "on", "sunday", "at", "18:00");
    private static final Map<String, Object> NIGHT = Map.of("from", "22:00", "to", "04:00");

    private static DataResult<DigestConfig> parse(Map<String, Object> config) {
        return DigestConfig.CODEC.parse(JavaOps.INSTANCE, config);
    }

    private static Map<String, Object> group(String id) {
        return Map.of("id", id, "stats", List.of("minecraft.mined:*"));
    }

    @Test
    void readsTheScheduleAndGroups() {
        final var config = parse(Map.of(
            "timezone", "Europe/London",
            "schedule", SCHEDULE,
            "night_hours", NIGHT,
            "groups", List.of(group("miner"))
        )).getOrThrow();

        assertEquals(ZoneId.of("Europe/London"), config.timezone());
        assertEquals(Schedule.weekly(DayOfWeek.SUNDAY, LocalTime.of(18, 0)), config.schedule());
        assertEquals(new NightHours(LocalTime.of(22, 0), LocalTime.of(4, 0)), config.nightHours());
        assertEquals(List.of("miner"), config.groups().stream().map(DigestConfig.StatGroup::id).toList());
    }

    @Test
    void defaultsToTheServersTimeZone() {
        final var config = parse(Map.of("schedule", SCHEDULE, "night_hours", NIGHT, "groups", List.of())).getOrThrow();

        assertEquals(ZoneId.systemDefault(), config.timezone());
    }

    @Test
    void refusesGroupIdsUsedMoreThanOnce() {
        assertTrue(parse(Map.of(
            "schedule",
            SCHEDULE,
            "night_hours",
            NIGHT,
            "groups",
            List.of(group("miner"), group("miner"))
        )).isError());
    }

    @Test
    void refusesGroupIdsTemplatesCantUse() {
        assertTrue(parse(Map.of(
            "schedule",
            SCHEDULE,
            "night_hours",
            NIGHT,
            "groups",
            List.of(group("miner.stone"))
        )).isError());
        assertTrue(parse(Map.of(
            "schedule",
            SCHEDULE,
            "night_hours",
            NIGHT,
            "groups",
            List.of(group("miner-stone"))
        )).isError());
    }

    @Test
    void refusesTimeZonesThatAreNotTimeZones() {
        assertTrue(parse(Map.of(
            "timezone",
            "Nowhere/Special",
            "schedule",
            SCHEDULE,
            "night_hours",
            NIGHT,
            "groups",
            List.of()
        )).isError());
    }

    @Test
    void refusesNightHoursThatStartWhenTheyEnd() {
        final var night = Map.of("from", "22:00", "to", "22:00");
        assertTrue(parse(Map.of("schedule", SCHEDULE, "night_hours", night, "groups", List.of())).isError());
    }
}
