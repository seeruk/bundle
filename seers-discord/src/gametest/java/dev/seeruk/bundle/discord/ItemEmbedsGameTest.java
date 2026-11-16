package dev.seeruk.bundle.discord;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.IntStream;

import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class ItemEmbedsGameTest {
    @GameTest
    public void linkedItemsShowTheirTooltip(GameTestHelper helper) {
        final var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Fancy Sword").withStyle(ChatFormatting.GOLD));
        sword.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("So sharp"))));
        sword.enchant(
            helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),
            5
        );

        final var embed = embeds(helper, sword).getFirst();

        helper.assertValueEqual(embed.getTitle(), "Fancy Sword", "title");
        helper.assertValueEqual(embed.getColorRaw(), colour(ChatFormatting.GOLD), "colour");
        helper.assertTrue(embed.getDescription().contains("Sharpness V"), "Should show the enchantment");
        // Lore is italic in game too
        helper.assertTrue(embed.getDescription().contains("*So sharp*"), "Should show the lore");
        helper.succeed();
    }

    @GameTest
    public void renamedItemsShowWhatTheyAre(GameTestHelper helper) {
        final var stick = new ItemStack(Items.STICK);
        stick.set(DataComponents.CUSTOM_NAME, Component.literal("Wand"));

        final var embed = embeds(helper, stick).getFirst();

        helper.assertValueEqual(embed.getTitle(), "Wand", "title");
        helper.assertValueEqual(embed.getFooter().getText(), "Stick", "footer");
        helper.succeed();
    }

    @GameTest
    public void attackDamageAndSpeedIncludeThePlayers(GameTestHelper helper) {
        final var description = embeds(helper, new ItemStack(Items.NETHERITE_SWORD)).getFirst().getDescription();

        // The sword adds 7 damage and takes 2.4 from the speed, to the player's own 1 and 4
        helper.assertTrue(description.contains("8 Attack Damage"), "Should show the attack damage");
        helper.assertTrue(description.contains("1.6 Attack Speed"), "Should show the attack speed");
        helper.succeed();
    }

    @GameTest
    public void itemsWithoutTheirOwnColourUseTheirRaritysColour(GameTestHelper helper) {
        final var embed = embeds(helper, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)).getFirst();

        helper.assertValueEqual(embed.getColorRaw(), colour(ChatFormatting.AQUA), "colour");
        helper.succeed();
    }

    @GameTest
    public void damagedItemsShowTheirDurability(GameTestHelper helper) {
        final var damaged = new ItemStack(Items.DIAMOND_SWORD);
        damaged.setDamageValue(100);

        helper.assertTrue(
            embeds(helper, damaged).getFirst().getDescription().contains("Durability: 1461 / 1561"),
            "Should show the durability"
        );
        helper.assertFalse(
            embeds(helper, new ItemStack(Items.DIAMOND_SWORD)).getFirst().getDescription().contains("Durability"),
            "Shouldn't show the durability of an undamaged item"
        );
        helper.succeed();
    }

    @GameTest
    public void stacksShowTheirCount(GameTestHelper helper) {
        final var stack = new ItemStack(Items.ENDER_PEARL, 16);
        final var single = new ItemStack(Items.ENDER_PEARL);

        helper.assertValueEqual(titles(embeds(helper, stack)), List.of("16 × Ender Pearl"), "stack's title");
        helper.assertValueEqual(titles(embeds(helper, single)), List.of("Ender Pearl"), "single item's title");
        helper.succeed();
    }

    @GameTest
    public void eachItemIsShownOnceInOrder(GameTestHelper helper) {
        final var sword = new ItemStack(Items.DIAMOND_SWORD);
        final var apple = new ItemStack(Items.GOLDEN_APPLE);

        helper.assertValueEqual(
            titles(embeds(helper, sword, apple, sword)),
            List.of("Diamond Sword", "Golden Apple"),
            "titles"
        );
        helper.succeed();
    }

    @GameTest
    public void messagesWithoutItemsHaveNoEmbeds(GameTestHelper helper) {
        helper.assertValueEqual(
            ItemEmbeds.of(Component.literal("Testing"), player(helper)),
            List.of(),
            "embeds"
        );
        helper.succeed();
    }

    @GameTest
    public void itemsWithTheirTooltipHiddenHaveNoEmbed(GameTestHelper helper) {
        final var hidden = new ItemStack(Items.DIAMOND_SWORD);
        hidden.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, new LinkedHashSet<>()));

        helper.assertValueEqual(embeds(helper, hidden), List.of(), "embeds");
        helper.succeed();
    }

    @GameTest
    public void atMostTenItemsAreShown(GameTestHelper helper) {
        final var items = BuiltInRegistries.ITEM.stream()
            .filter(item -> item != Items.AIR)
            .limit(Message.MAX_EMBED_COUNT + 1)
            .map(ItemStack::new)
            .toArray(ItemStack[]::new);

        helper.assertValueEqual(embeds(helper, items).size(), Message.MAX_EMBED_COUNT, "embeds");
        helper.succeed();
    }

    @GameTest
    public void embedsStopBeforeDiscordsLimitForAMessage(GameTestHelper helper) {
        // Each has half the limit in lore, so with their titles etc., two are more than Discord allows
        final var lore = new ItemLore(List.of(Component.literal("a".repeat(MessageEmbed.EMBED_MAX_LENGTH_BOT / 2))));
        final var items = IntStream.range(0, 2)
            .mapToObj(i -> {
                final var stack = new ItemStack(Items.NETHERITE_SWORD);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("Test Item %d".formatted(i)));
                stack.set(DataComponents.LORE, lore);
                return stack;
            })
            .toArray(ItemStack[]::new);

        helper.assertValueEqual(titles(embeds(helper, items)), List.of("Test Item 0"), "titles");
        helper.succeed();
    }

    /**
     * The embeds for a message linking the items, as Vanilla (and the Chat mod) links them.
     */
    private static List<MessageEmbed> embeds(GameTestHelper helper, ItemStack... items) {
        final var content = Component.empty();
        Arrays.stream(items).forEach(item -> content.append(item.getDisplayName()).append(" "));
        return ItemEmbeds.of(content, player(helper));
    }

    private static List<String> titles(List<MessageEmbed> embeds) {
        return embeds.stream().map(MessageEmbed::getTitle).toList();
    }

    private static int colour(ChatFormatting formatting) {
        return TextColor.fromLegacyFormat(formatting).getValue();
    }
}
