package dev.seeruk.bundle.propswap.swap;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WallSwapTest {
    private static final WallSwap SWAP = new WallSwap();

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    class Supports {
        static Stream<Arguments> shouldSupport() {
            return Stream.of(
                Arguments.of("Supports stone walls", Blocks.COBBLESTONE_WALL.defaultBlockState(), true),
                Arguments.of("Supports brick walls", Blocks.MUD_BRICK_WALL.defaultBlockState(), true),
                Arguments.of("Doesn't support fences", Blocks.OAK_FENCE.defaultBlockState(), false),
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
        static Stream<Arguments> shouldCycleSide() {
            final var north = WallBlock.NORTH;
            return Stream.of(
                Arguments.of(
                    "Cycles a side from none to low",
                    wall(true, WallSide.NONE), Direction.NORTH,
                    SwapResult.changed(wall(true, WallSide.LOW), north)
                ),
                Arguments.of(
                    "Cycles a side from low to tall",
                    wall(true, WallSide.LOW), Direction.NORTH,
                    SwapResult.changed(wall(true, WallSide.TALL), north)
                ),
                Arguments.of(
                    "Cycles a side from tall back to none",
                    wall(true, WallSide.TALL), Direction.NORTH,
                    SwapResult.changed(wall(true, WallSide.NONE), north)
                ),
                Arguments.of(
                    "Adds the post when removing the last side",
                    wall(false, WallSide.TALL), Direction.NORTH,
                    SwapResult.changed(wall(true, WallSide.NONE), north)
                ),
                Arguments.of(
                    "Cycles the side faced when clicking the top",
                    wall(true), Direction.UP,
                    SwapResult.changed(wall(true, WallSide.NONE, WallSide.LOW), WallBlock.EAST)
                ),
                Arguments.of(
                    "Cycles the side faced when clicking the bottom",
                    wall(true), Direction.DOWN,
                    SwapResult.changed(wall(true, WallSide.NONE, WallSide.LOW), WallBlock.EAST)
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldCycleSide(
            final String description,
            final BlockState state,
            final Direction face,
            final SwapResult expected
        ) {
            final var result = SWAP.swap(new SwapInput(state, face, Direction.EAST, false));

            assertEquals(expected, result.orElseThrow());
        }

        static Stream<Arguments> shouldTogglePostWhileCrouchingOnASide() {
            return Stream.of(
                Arguments.of(
                    "Removes the post",
                    wall(true, WallSide.LOW),
                    SwapResult.changed(wall(false, WallSide.LOW), WallBlock.UP)
                ),
                Arguments.of(
                    "Adds the post",
                    wall(false, WallSide.LOW),
                    SwapResult.changed(wall(true, WallSide.LOW), WallBlock.UP)
                ),
                Arguments.of(
                    "Refuses to remove the post without any sides",
                    wall(true),
                    new SwapResult(wall(true), Component.literal("A wall needs a post or at least one side"))
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldTogglePostWhileCrouchingOnASide(
            final String description,
            final BlockState state,
            final SwapResult expected
        ) {
            final var result = SWAP.swap(new SwapInput(state, Direction.NORTH, Direction.EAST, true));

            assertEquals(expected, result.orElseThrow());
        }

        static Stream<Arguments> shouldChangeAllSidesWhileCrouchingOnTopOrBottom() {
            return Stream.of(
                Arguments.of(
                    "Adds low sides when there are none",
                    wall(true), Direction.UP,
                    wall(true, WallSide.LOW, WallSide.LOW, WallSide.LOW, WallSide.LOW), "low"
                ),
                Arguments.of(
                    "Removes every side when some are there",
                    wall(true, WallSide.TALL), Direction.UP,
                    wall(true), "none"
                ),
                Arguments.of(
                    "Adds the post when removing every side",
                    wall(false, WallSide.LOW), Direction.UP,
                    wall(true), "none"
                ),
                Arguments.of(
                    "Works when clicking the bottom too",
                    wall(true, WallSide.TALL), Direction.DOWN,
                    wall(true), "none"
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldChangeAllSidesWhileCrouchingOnTopOrBottom(
            final String description,
            final BlockState state,
            final Direction face,
            final BlockState expected,
            final String value
        ) {
            final var result = SWAP.swap(new SwapInput(state, face, Direction.EAST, true));

            assertEquals(SwapResult.changed(expected, "sides", value), result.orElseThrow());
        }
    }

    /**
     * A wall with the given post and sides, in north, east, south, west order. Missing sides are
     * none.
     */
    private static BlockState wall(boolean post, WallSide... sides) {
        final var directions = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

        var state = Blocks.COBBLESTONE_WALL.defaultBlockState().setValue(WallBlock.UP, post);
        for (var i = 0; i < sides.length; i++) {
            state = state.setValue(WallBlock.PROPERTY_BY_DIRECTION.get(directions[i]), sides[i]);
        }
        return state;
    }
}
