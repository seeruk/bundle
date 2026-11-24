package dev.seeruk.bundle.core.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A set of stats chosen by name patterns, e.g. {@code minecraft.mined:*} for every block mined, or
 * {@code scoreboard:seers_chat.messages_sent} for a scoreboard objective. Vanilla stats use their
 * scoreboard criterion names. A {@code *} matches any run of characters, and a pattern starting
 * with {@code !} excludes whatever it matches, whatever order the patterns are in.
 */
public final class StatSelection {
    public static final Codec<StatSelection> CODEC = Codec.STRING.listOf()
        .comapFlatMap(StatSelection::parse, StatSelection::patterns);

    private final List<String> patterns;
    private final List<Pattern> included;
    private final List<Pattern> excluded;
    // Concurrent, as a selection from config can be used from any thread
    private final Map<String, Boolean> matches = new ConcurrentHashMap<>();

    private StatSelection(List<String> patterns) {
        this.patterns = List.copyOf(patterns);
        this.included = patterns.stream().filter(p -> !p.startsWith("!")).map(StatSelection::glob).toList();
        this.excluded = patterns.stream().filter(p -> p.startsWith("!")).map(p -> glob(p.substring(1))).toList();
    }

    /**
     * Throws if any pattern is blank, or is only a {@code !}.
     */
    public static StatSelection of(List<String> patterns) {
        return parse(patterns).getOrThrow(IllegalArgumentException::new);
    }

    private static DataResult<StatSelection> parse(List<String> patterns) {
        return patterns.stream()
            .filter(p -> p.isBlank() || p.equals("!"))
            .findFirst()
            .map(p -> DataResult.<StatSelection>error(() -> "Stat pattern can't be empty: '" + p + "'"))
            .orElseGet(() -> DataResult.success(new StatSelection(patterns)));
    }

    private static Pattern glob(String pattern) {
        return Pattern.compile(Arrays.stream(pattern.split("\\*", -1))
            .map(Pattern::quote)
            .collect(Collectors.joining(".*")));
    }

    public List<String> patterns() {
        return patterns;
    }

    /**
     * The patterns without a {@code *} (and without any {@code !}), which should each name a real
     * stat, so a typo in one can be warned about.
     */
    public List<String> exactNames() {
        return patterns.stream()
            .map(p -> p.startsWith("!") ? p.substring(1) : p)
            .filter(p -> !p.contains("*"))
            .toList();
    }

    public boolean matches(String name) {
        return matches.computeIfAbsent(
            name, n ->
                included.stream().anyMatch(p -> p.matcher(n).matches())
                    && excluded.stream().noneMatch(p -> p.matcher(n).matches())
        );
    }

    /**
     * Adds up the values of every selected stat.
     */
    public long sum(Map<String, Integer> values) {
        return values.entrySet().stream()
            .filter(e -> matches(e.getKey()))
            .mapToLong(Map.Entry::getValue)
            .sum();
    }
}
