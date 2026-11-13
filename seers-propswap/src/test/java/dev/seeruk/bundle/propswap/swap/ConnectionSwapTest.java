package dev.seeruk.bundle.propswap.swap;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConnectionSwapTest {
    private static final ConnectionSwap SWAP = new ConnectionSwap();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Supports {
        static Stream<Arguments> shouldSupport() {
            final var copperBars = Blocks.COPPER_BARS.asList().stream().map(block -> Arguments.of(
                "Supports " + BuiltInRegistries.BLOCK.getKey(block).getPath(),
                block.defaultBlockState(),
                true
            ));

            return Stream.concat(copperBars, Stream.of(
                Arguments.of("Supports wooden fences", Blocks.OAK_FENCE.defaultBlockState(), true),
                Arguments.of("Supports nether brick fences", Blocks.NETHER_BRICK_FENCE.defaultBlockState(), true),
                Arguments.of("Supports glass panes", Blocks.GLASS_PANE.defaultBlockState(), true),
                Arguments.of("Supports stained glass panes", Blocks.STAINED_GLASS_PANE.pick(DyeColor.RED).defaultBlockState(), true),
                Arguments.of("Supports iron bars", Blocks.IRON_BARS.defaultBlockState(), true),
                Arguments.of("Doesn't support walls", Blocks.COBBLESTONE_WALL.defaultBlockState(), false),
                Arguments.of("Doesn't support other blocks", Blocks.STONE.defaultBlockState(), false)
            ));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldSupport(final String description, final BlockState state, final boolean expected) {
            assertEquals(expected, SWAP.supports(state));
        }
    }

    @Nested
    class Swap {
        static Stream<Arguments> shouldToggleSide() {
            return Stream.of(
                Arguments.of("Connects the clicked side", fence(), Direction.NORTH, Direction.EAST, Direction.NORTH),
                Arguments.of("Disconnects the clicked side", fence(Direction.WEST), Direction.WEST, Direction.EAST, Direction.WEST),
                Arguments.of("Toggles the side faced when clicking the top", fence(), Direction.UP, Direction.EAST, Direction.EAST),
                Arguments.of("Toggles the side faced when clicking the bottom", fence(), Direction.DOWN, Direction.SOUTH, Direction.SOUTH)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldToggleSide(
            final String description,
            final BlockState state,
            final Direction face,
            final Direction facing,
            final Direction expected
        ) {
            final var property = CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(expected);

            final var result = SWAP.swap(new SwapInput(state, face, facing, false));

            assertEquals(SwapResult.changed(state.cycle(property), property).locked(), result.orElseThrow());
        }

        static Stream<Arguments> shouldToggleAllSidesWhileCrouching() {
            final var all = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            return Stream.of(
                Arguments.of("Connects every side when none are connected", fence(), fence(all), "true"),
                Arguments.of("Disconnects every side when some are connected", fence(Direction.NORTH), fence(), "false"),
                Arguments.of("Disconnects every side when all are connected", fence(all), fence(), "false")
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldToggleAllSidesWhileCrouching(
            final String description,
            final BlockState state,
            final BlockState expected,
            final String value
        ) {
            final var result = SWAP.swap(new SwapInput(state, Direction.NORTH, Direction.NORTH, true));

            assertEquals(SwapResult.changed(expected, "sides", value).locked(), result.orElseThrow());
        }
    }

    private static BlockState fence(Direction... connected) {
        var state = Blocks.OAK_FENCE.defaultBlockState();
        for (final var side : connected) {
            state = state.setValue(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(side), true);
        }
        return state;
    }
}
