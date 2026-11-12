package dev.seeruk.bundle.propswap.swap;

import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MushroomSwapTest {
    private static final MushroomSwap SWAP = new MushroomSwap();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Supports {
        static Stream<Arguments> shouldSupport() {
            return Stream.of(
                Arguments.of("Supports brown mushroom blocks", Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState(), true),
                Arguments.of("Supports red mushroom blocks", Blocks.RED_MUSHROOM_BLOCK.defaultBlockState(), true),
                Arguments.of("Supports mushroom stems", Blocks.MUSHROOM_STEM.defaultBlockState(), true),
                Arguments.of("Doesn't support other blocks", Blocks.STONE.defaultBlockState(), false)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldSupport(final String description, final BlockState state, final boolean expected) {
            assertEquals(expected, SWAP.supports(state));
        }
    }

    @Nested
    class Swap {
        static Stream<Arguments> shouldToggleClickedFace() {
            return Stream.of(
                Arguments.of("Toggles the top", Direction.UP, true, false, false),
                Arguments.of("Toggles a side", Direction.NORTH, true, false, false),
                Arguments.of("Toggles the bottom back to skin", Direction.DOWN, false, false, true),
                Arguments.of("Ignores crouching", Direction.UP, true, true, false)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldToggleClickedFace(
            final String description,
            final Direction face,
            final boolean skin,
            final boolean crouching,
            final boolean expected
        ) {
            final var property = PipeBlock.PROPERTY_BY_DIRECTION.get(face);
            final var state = Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState().setValue(property, skin);

            final var result = SWAP.swap(new SwapInput(state, face, Direction.NORTH, crouching));

            assertEquals(SwapResult.changed(state.setValue(property, expected), property), result.orElseThrow());
        }
    }
}
