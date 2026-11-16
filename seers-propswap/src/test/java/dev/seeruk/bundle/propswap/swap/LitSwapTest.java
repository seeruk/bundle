package dev.seeruk.bundle.propswap.swap;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LitSwapTest {
    private static final LitSwap SWAP = new LitSwap();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Supports {
        static Stream<Arguments> shouldSupport() {
            return Stream.of(
                Arguments.of("Supports furnaces", Blocks.FURNACE.defaultBlockState(), true),
                Arguments.of("Supports smokers", Blocks.SMOKER.defaultBlockState(), true),
                Arguments.of("Supports blast furnaces", Blocks.BLAST_FURNACE.defaultBlockState(), true),
                Arguments.of(
                    "Doesn't support other blocks with a lit state",
                    Blocks.CAMPFIRE.defaultBlockState(),
                    false
                ),
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
        static Stream<Arguments> shouldToggleLit() {
            return Stream.of(
                Arguments.of("Lights an unlit furnace", false, true),
                Arguments.of("Unlights a lit furnace", true, false)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldToggleLit(final String description, final boolean lit, final boolean expected) {
            final var state = Blocks.FURNACE.defaultBlockState().setValue(AbstractFurnaceBlock.LIT, lit);

            final var result = SWAP.swap(new SwapInput(state, Direction.UP, Direction.NORTH, false));

            assertEquals(
                SwapResult.changed(state.setValue(AbstractFurnaceBlock.LIT, expected), AbstractFurnaceBlock.LIT),
                result.orElseThrow()
            );
        }
    }
}
