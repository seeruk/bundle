package dev.seeruk.bundle.digest.book;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * What a book's section shows an entry for each item of: one of the built-in lists, written as
 * {@code list: deaths}, or a group's players, highest first, written as {@code ranking: miner}.
 *
 * @param kind  Which list it is.
 * @param group The group, for {@link Kind#RANKING}.
 */
public record ListSource(Kind kind, Optional<String> group) {
    // Read from whichever of `list` or `ranking` the section has, which has to be one of them
    static final MapCodec<ListSource> MAP_CODEC = RecordCodecBuilder.<Pair<Optional<Kind>, Optional<String>>>mapCodec(instance -> instance.group(
        Kind.LIST_CODEC.optionalFieldOf("list").forGetter(Pair::getFirst),
        Codec.STRING.optionalFieldOf("ranking").forGetter(Pair::getSecond)
    ).apply(instance, Pair::of)).flatXmap(
        written -> {
            if (written.getFirst().isPresent() && written.getSecond().isPresent()) {
                return DataResult.error(() -> "A section can't have both a list and a ranking");
            }
            if (written.getFirst().isEmpty() && written.getSecond().isEmpty()) {
                return DataResult.error(() -> "A list section needs a list or a ranking");
            }
            return DataResult.success(written.getSecond()
                .map(group -> new ListSource(Kind.RANKING, Optional.of(group)))
                .orElseGet(() -> new ListSource(written.getFirst().orElseThrow(), Optional.empty())));
        },
        source -> DataResult.success(source.kind == Kind.RANKING
            ? Pair.of(Optional.empty(), source.group)
            : Pair.of(Optional.of(source.kind), Optional.empty()))
    );

    public enum Kind {
        DEATHS,
        ADVANCEMENTS,
        FIRSTS,
        NEW_PLAYERS,
        LEADERS,
        /** A group's players, written with {@code ranking} rather than as a list. */
        RANKING;

        // The lists written with `list`, which are every kind but a ranking
        private static final Codec<Kind> LIST_CODEC = Codec.STRING.comapFlatMap(Kind::parse, Kind::written);

        private static DataResult<Kind> parse(String written) {
            return Arrays.stream(values())
                .filter(kind -> kind != RANKING && kind.written().equals(written))
                .findFirst()
                .map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "Not a list: " + written + ". The lists are " + Arrays.stream(values())
                    .filter(kind -> kind != RANKING)
                    .map(Kind::written)
                    .collect(Collectors.joining(", "))));
        }

        private String written() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
