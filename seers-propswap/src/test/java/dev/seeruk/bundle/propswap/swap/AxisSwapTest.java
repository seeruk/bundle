package dev.seeruk.bundle.propswap.swap;

import net.minecraft.world.level.block.RotatedPillarBlock;
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

public class AxisSwapTest {
    private static final AxisSwap SWAP = new AxisSwap();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Supports {
        static Stream<Arguments> shouldSupport() {
            return Stream.of(
                Arguments.of("Supports logs", Blocks.OAK_LOG.defaultBlockState(), true),
                Arguments.of("Supports stone pillars", Blocks.QUARTZ_PILLAR.defaultBlockState(), true),
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
        static Stream<Arguments> shouldCycleAxis() {
            return Stream.of(
                Arguments.of("Cycles from X to Y", Direction.Axis.X, false, Direction.Axis.Y),
                Arguments.of("Cycles from Y to Z", Direction.Axis.Y, false, Direction.Axis.Z),
                Arguments.of("Cycles from Z back to X", Direction.Axis.Z, false, Direction.Axis.X),
                Arguments.of("Cycles backwards while crouching", Direction.Axis.Y, true, Direction.Axis.X),
                Arguments.of("Cycles backwards from X to Z", Direction.Axis.X, true, Direction.Axis.Z)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldCycleAxis(
            final String description,
            final Direction.Axis axis,
            final boolean crouching,
            final Direction.Axis expected
        ) {
            final var state = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);

            final var result = SWAP.swap(new SwapInput(state, Direction.UP, Direction.NORTH, crouching));

            assertEquals(
                SwapResult.changed(state.setValue(RotatedPillarBlock.AXIS, expected), RotatedPillarBlock.AXIS),
                result.orElseThrow()
            );
        }
    }
}
