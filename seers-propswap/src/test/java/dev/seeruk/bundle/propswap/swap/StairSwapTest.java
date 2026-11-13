package dev.seeruk.bundle.propswap.swap;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.StairsShape;
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

public class StairSwapTest {
    private static final StairSwap SWAP = new StairSwap();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Supports {
        static Stream<Arguments> shouldSupport() {
            return Stream.of(
                Arguments.of("Supports wooden stairs", Blocks.OAK_STAIRS.defaultBlockState(), true),
                Arguments.of("Supports stone stairs", Blocks.STONE_STAIRS.defaultBlockState(), true),
                Arguments.of("Doesn't support other blocks", Blocks.OAK_SLAB.defaultBlockState(), false)
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
        static Stream<Arguments> shouldCycleShape() {
            return Stream.of(
                Arguments.of("Cycles from straight to an inner corner", StairsShape.STRAIGHT, false, StairsShape.INNER_LEFT),
                Arguments.of("Cycles from inner to outer corners", StairsShape.INNER_RIGHT, false, StairsShape.OUTER_LEFT),
                Arguments.of("Cycles from the last corner back to straight", StairsShape.OUTER_RIGHT, false, StairsShape.STRAIGHT),
                Arguments.of("Cycles backwards while crouching", StairsShape.INNER_LEFT, true, StairsShape.STRAIGHT),
                Arguments.of("Cycles backwards from straight to the last corner", StairsShape.STRAIGHT, true, StairsShape.OUTER_RIGHT)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldCycleShape(
            final String description,
            final StairsShape shape,
            final boolean crouching,
            final StairsShape expected
        ) {
            final var state = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.SHAPE, shape);

            final var result = SWAP.swap(new SwapInput(state, Direction.UP, Direction.NORTH, crouching));

            assertEquals(
                SwapResult.changed(state.setValue(StairBlock.SHAPE, expected), StairBlock.SHAPE).locked(),
                result.orElseThrow()
            );
        }
    }
}
