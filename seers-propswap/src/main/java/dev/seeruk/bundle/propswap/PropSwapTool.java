package dev.seeruk.bundle.propswap;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * The tools PropSwap works with. Each tool changes the blocks in its
 * {@code seers-propswap:tool/<tool>} tag, which includes {@code minecraft:mineable/<tool>}.
 */
public enum PropSwapTool {
    AXE(ItemTags.AXES, "axe"),
    PICKAXE(ItemTags.PICKAXES, "pickaxe"),
    SHOVEL(ItemTags.SHOVELS, "shovel"),
    HOE(ItemTags.HOES, "hoe");

    private final TagKey<Item> items;
    private final TagKey<Block> blocks;

    PropSwapTool(TagKey<Item> items, String name) {
        this.items = items;
        this.blocks = TagKey.create(Registries.BLOCK, SeersPropSwap.id("tool/" + name));
    }

    /**
     * Whether the item is a tool that can change the block. Items can count as more than one tool
     * (e.g. a modded paxel).
     */
    public static boolean canChange(ItemStack stack, BlockState state) {
        return Arrays.stream(values()).anyMatch(tool -> stack.is(tool.items) && state.is(tool.blocks));
    }

    public static boolean isTool(ItemStack stack) {
        return Arrays.stream(values()).anyMatch(tool -> stack.is(tool.items));
    }
}
