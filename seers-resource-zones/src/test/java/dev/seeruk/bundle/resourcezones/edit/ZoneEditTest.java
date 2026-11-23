package dev.seeruk.bundle.resourcezones.edit;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ZoneEditTest {
    private static final ChunkPos CHUNK = new ChunkPos(1, -2);
    private static final ChunkPos OTHER = new ChunkPos(0, 0);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Mark {
        @Test
        void addsAChunkThatIsntInAZone() {
            final var edit = ZoneEdit.start(EditMode.ADD).mark(Level.OVERWORLD, CHUNK, Set.of());

            assertEquals(Map.of(CHUNK, true), edit.in(Level.OVERWORLD));
        }

        @Test
        void removesAChunkThatsInAZone() {
            final var edit = ZoneEdit.start(EditMode.REMOVE).mark(Level.OVERWORLD, CHUNK, Set.of(CHUNK));

            assertEquals(Map.of(CHUNK, false), edit.in(Level.OVERWORLD));
        }

        @Test
        void changesNothingForAChunkThatsAlreadyAsMarked() {
            final var added = ZoneEdit.start(EditMode.ADD).mark(Level.OVERWORLD, CHUNK, Set.of(CHUNK));
            final var removed = ZoneEdit.start(EditMode.REMOVE).mark(Level.OVERWORLD, CHUNK, Set.of());

            assertEquals(Map.of(), added.pending());
            assertEquals(Map.of(), removed.pending());
        }

        @Test
        void undoesAChangeWhenMarkedInTheOtherMode() {
            final var edit = ZoneEdit.start(EditMode.ADD)
                .mark(Level.OVERWORLD, CHUNK, Set.of())
                .mark(Level.OVERWORLD, OTHER, Set.of())
                .withMode(EditMode.REMOVE)
                .mark(Level.OVERWORLD, CHUNK, Set.of());

            assertEquals(Map.of(OTHER, true), edit.in(Level.OVERWORLD));
        }

        @Test
        void changesNothingWhilePaused() {
            final var added = ZoneEdit.start(EditMode.PAUSED).mark(Level.OVERWORLD, CHUNK, Set.of());
            final var removed = ZoneEdit.start(EditMode.PAUSED).mark(Level.OVERWORLD, CHUNK, Set.of(CHUNK));

            assertEquals(Map.of(), added.pending());
            assertEquals(Map.of(), removed.pending());
        }

        @Test
        void keepsDimensionsApart() {
            final var edit = ZoneEdit.start(EditMode.ADD).mark(Level.NETHER, CHUNK, Set.of());

            assertEquals(Map.of(), edit.in(Level.OVERWORLD));
            assertEquals(Map.of(CHUNK, true), edit.in(Level.NETHER));
        }
    }

    @Nested
    class Changes {
        private static final ZoneEdit EDIT = ZoneEdit.start(EditMode.ADD)
            .mark(Level.OVERWORLD, CHUNK, Set.of())
            .mark(Level.NETHER, CHUNK, Set.of())
            .withMode(EditMode.REMOVE)
            .mark(Level.OVERWORLD, OTHER, Set.of(OTHER));

        @Test
        void splitsEachDimensionsAddsAndRemoves() {
            assertEquals(Set.of(CHUNK), EDIT.added(Level.OVERWORLD));
            assertEquals(Set.of(OTHER), EDIT.removed(Level.OVERWORLD));
            assertEquals(Set.of(), EDIT.removed(Level.NETHER));
        }

        @Test
        void countsEveryDimension() {
            assertEquals(2, EDIT.added());
            assertEquals(1, EDIT.removed());
        }
    }
}
