package dev.seeruk.bundle.resourcezones.reset;

import com.mojang.serialization.JavaOps;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ResetHistoryTest {
    private static final ChunkPos CHUNK = new ChunkPos(1, -2);
    private static final ChunkPos OTHER = new ChunkPos(0, 0);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class WithRefreshedGenerated {
        @Test
        void addsExistingZoneChunks() {
            final var history = ResetHistory.EMPTY
                .withRefreshedGenerated(Level.OVERWORLD, Set.of(CHUNK), Set.of(CHUNK, OTHER));

            assertEquals(Set.of(CHUNK), history.generated().in(Level.OVERWORLD));
        }

        @Test
        void dropsChunksNoLongerInAZone() {
            final var history = ResetHistory.EMPTY
                .withGenerated(Level.OVERWORLD, CHUNK)
                .withGenerated(Level.OVERWORLD, OTHER)
                .withRefreshedGenerated(Level.OVERWORLD, Set.of(OTHER), Set.of());

            assertEquals(Set.of(OTHER), history.generated().in(Level.OVERWORLD));
        }
    }

    @Test
    void findsGeneratedChunksThatArentStored() {
        final var history = ResetHistory.EMPTY
            .withGenerated(Level.OVERWORLD, CHUNK)
            .withGenerated(Level.OVERWORLD, OTHER);

        assertEquals(Set.of(CHUNK), history.missing(Level.OVERWORLD, Set.of(OTHER)));
    }

    @Nested
    class WithReset {
        private static final ResetHistory RESET = ResetHistory.EMPTY
            .withGenerated(Level.OVERWORLD, CHUNK)
            .withGenerated(Level.OVERWORLD, OTHER)
            .withReset(Map.of(Level.OVERWORLD, Set.of(CHUNK)));

        @Test
        void countsTheReset() {
            assertEquals(1, RESET.resets());
        }

        @Test
        void forgetsResetChunksWereGenerated() {
            assertEquals(Set.of(OTHER), RESET.generated().in(Level.OVERWORLD));
        }

        @Test
        void recordsWhenEachChunkWasReset() {
            final var again = RESET.withReset(Map.of(Level.OVERWORLD, Set.of(OTHER)));

            assertEquals(Map.of(Level.OVERWORLD, Map.of(CHUNK, 1, OTHER, 2)), again.resetAt());
        }

        @Test
        void changesNothingWithoutResetChunks() {
            assertSame(RESET, RESET.withReset(Map.of(Level.OVERWORLD, Set.of())));
        }
    }

    @Test
    void tellsWhetherAChunkWasResetSinceAGivenCount() {
        final var history = ResetHistory.EMPTY.withReset(Map.of(Level.OVERWORLD, Set.of(CHUNK)));

        assertTrue(history.wasResetSince(Level.OVERWORLD, CHUNK, 0), "Not reset since before it was");
        assertFalse(history.wasResetSince(Level.OVERWORLD, CHUNK, 1), "Reset since it was");
        assertFalse(history.wasResetSince(Level.OVERWORLD, OTHER, 0), "Never-reset chunk was reset");
        assertFalse(history.wasResetSince(Level.NETHER, CHUNK, 0), "Reset in another dimension");
    }

    @Test
    void savesAndLoads() {
        final var history = ResetHistory.EMPTY
            .withGenerated(Level.OVERWORLD, OTHER)
            .withGenerated(Level.NETHER, CHUNK)
            .withReset(Map.of(Level.NETHER, Set.of(CHUNK)));

        final var saved = ResetHistory.CODEC.encodeStart(JavaOps.INSTANCE, history).getOrThrow();

        assertEquals(history, ResetHistory.CODEC.parse(JavaOps.INSTANCE, saved).getOrThrow());
    }
}
