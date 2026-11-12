package dev.seeruk.bundle.propswap.swap;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Describes how a tool was used on a block, as seen by a {@link Swap}.
 *
 * @param state     The state of the block that was clicked.
 * @param face      The face of the block that was clicked.
 * @param facing    The horizontal direction the player is facing.
 * @param crouching Whether the player was crouching when the block was clicked.
 */
public record SwapInput(BlockState state, Direction face, Direction facing, boolean crouching) {}
