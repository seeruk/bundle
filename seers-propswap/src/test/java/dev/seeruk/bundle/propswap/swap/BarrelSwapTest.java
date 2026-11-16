package dev.seeruk.bundle.propswap.swap;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BarrelSwapTest {
    private static final BarrelSwap SWAP = new BarrelSwap();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Supports {
        static Stream<Arguments> shouldSupport() {
            return Stream.of(
                Arguments.of("Supports barrels", Blocks.BARREL.defaultBlockState(), true),
                Arguments.of("Doesn't support other containers", Blocks.CHEST.defaultBlockState(), false)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldSupport(final String description, final BlockState state, final boolean expected) {
            assertEquals(expected, SWAP.supports(state));
        }
    }

    @Nested
    class Rotate {
        static Stream<Arguments> shouldRotate() {
            return Stream.of(
                Arguments.of("Faces the clicked side", Direction.UP, Direction.NORTH, Direction.NORTH),
                Arguments.of(
                    "Faces the opposite way when clicking the side it faces",
                    Direction.NORTH,
                    Direction.NORTH,
                    Direction.SOUTH
                ),
                Arguments.of("Faces down when clicking the bottom", Direction.EAST, Direction.DOWN, Direction.DOWN)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldRotate(final String description, final Direction facing, final Direction face, final Direction expected) {
            final var state = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, facing);

            final var result = BarrelSwap.rotate(new SwapInput(state, face, Direction.NORTH, false));

            assertEquals(SwapResult.changed(state.setValue(BarrelBlock.FACING, expected), BarrelBlock.FACING), result);
        }
    }
}
