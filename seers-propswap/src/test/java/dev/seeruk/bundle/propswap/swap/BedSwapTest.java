package dev.seeruk.bundle.propswap.swap;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BedSwapTest {
    private static final BedSwap SWAP = new BedSwap();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Supports {
        static Stream<Arguments> shouldSupport() {
            return Stream.of(
                Arguments.of("Supports coloured beds", Blocks.BED.pick(DyeColor.RED).defaultBlockState(), true),
                Arguments.of("Supports straw beds", Blocks.STRAW_BED.defaultBlockState(), true),
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
    class Flip {
        static Stream<Arguments> shouldFlipHalf() {
            return Stream.of(
                Arguments.of("Turns a foot into a head", bed(BedPart.FOOT, Direction.NORTH), bed(BedPart.HEAD, Direction.SOUTH)),
                Arguments.of("Turns a head into a foot", bed(BedPart.HEAD, Direction.EAST), bed(BedPart.FOOT, Direction.WEST))
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldFlipHalf(final String description, final BlockState state, final BlockState expected) {
            assertEquals(SwapResult.changed(expected, AbstractBedBlock.PART).locked(), BedSwap.flip(state));
        }
    }

    private static BlockState bed(BedPart part, Direction facing) {
        return Blocks.BED.pick(DyeColor.RED).defaultBlockState()
            .setValue(AbstractBedBlock.PART, part)
            .setValue(AbstractBedBlock.FACING, facing);
    }
}
