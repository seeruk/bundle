package dev.seeruk.bundle.digest;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import dev.seeruk.bundle.core.stats.StatUnit;
import dev.seeruk.bundle.core.time.Schedule;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DigestConfigTest {
    private static final Map<String, Object> SCHEDULE = Map.of("every", "week", "on", "sunday", "at", "18:00");
    private static final Map<String, Object> NIGHT = Map.of("from", "22:00", "to", "04:00");
    private static final Map<String, Object> BOOK = Map.of(
        "title", "Seer's Digest",
        "author", "The Editors",
        "date_format", "d MMM yyyy",
        "cover", "${title}"
    );

    // A config with everything it needs, with the given settings replacing those
    private static DataResult<DigestConfig> parse(Map<String, Object> settings) {
        final var config = new HashMap<String, Object>(Map.of(
            "schedule", SCHEDULE,
            "night_hours", NIGHT,
            "groups", List.of(),
            "book", BOOK
        ));
        config.putAll(settings);
        return DigestConfig.CODEC.parse(JavaOps.INSTANCE, config);
    }

    private static Map<String, Object> group(String id) {
        return Map.of("id", id, "stats", List.of("minecraft.mined:*"));
    }

    private static Map<String, Object> book(String key, String value) {
        final var book = new HashMap<>(BOOK);
        book.put(key, value);
        return Map.of("book", book);
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
        assertEquals("Seer's Digest", config.book().title());
        assertEquals("The Editors", config.book().author());
        assertEquals("d MMM yyyy", config.book().dateFormat());
        assertEquals("${title}", config.book().cover().template());
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
    void refusesBookTitlesLongerThanVanillaAllows() {
        assertTrue(parse(book("title", "a".repeat(32))).isSuccess());
        assertTrue(parse(book("title", "a".repeat(33))).isError());
    }

    @Test
    void refusesDateFormatsThatAreNotDateFormats() {
        assertTrue(parse(book("date_format", "d MMM {")).isError());
    }
}
