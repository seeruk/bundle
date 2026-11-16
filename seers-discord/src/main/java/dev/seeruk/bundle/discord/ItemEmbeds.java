package dev.seeruk.bundle.discord;

import dev.seeruk.bundle.text.TextComponents;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilities for creating Discord embeds for the items linked in a messages (i.e. within a component
 * tree), showing the same information found in their in-game tooltips (and a bit more).
 */
public final class ItemEmbeds {
    private ItemEmbeds() {}

    /**
     * Returns a list of embeds, one for each unique item found within the content component tree.
     * Limited to the maximum number of embeds that Discord allows in one message.
     *
     * @param content The message's content, with items linked as Vanilla links them (see
     *                {@link net.minecraft.world.item.ItemStack#getDisplayName}).
     * @param player  The player to use to provide context for the item tooltip being generated,
     *                as some of their numbers depend on the player (e.g. attack damage includes the
     *                player's own).
     */
    public static List<MessageEmbed> of(Component content, Player player) {
        final var context = Item.TooltipContext.of(player.level());
        final var embeds = new ArrayList<MessageEmbed>();
        var length = 0;

        for (final var item : linkedItems(content)) {
            final var embed = embed(item, context, player);
            if (embed.isEmpty()) {
                continue;
            }

            length += embed.get().getLength();
            if (embeds.size() == Message.MAX_EMBED_COUNT || length > MessageEmbed.EMBED_MAX_LENGTH_BOT) {
                break;
            }
            embeds.add(embed.get());
        }

        return embeds;
    }

    private static SequencedSet<ItemStackTemplate> linkedItems(Component content) {
        final var items = new LinkedHashSet<ItemStackTemplate>();
        content.visit(
            (style, text) -> {
                if (style.getHoverEvent() instanceof HoverEvent.ShowItem(var item)) {
                    items.add(item);
                }
                return Optional.empty();
            }, Style.EMPTY
        );
        return items;
    }

    /**
     * The item's tooltip as an embed, with its durability if it's damaged, its count if there's
     * more than one, and the item's own name at the bottom. Items with their tooltip hidden don't
     * get one.
     */
    private static Optional<MessageEmbed> embed(ItemStackTemplate item, Item.TooltipContext context, Player player) {
        final var stack = item.create();
        final var tooltip = stack.getTooltipLines(context, player, TooltipFlag.NORMAL);
        if (stack.isEmpty() || tooltip.isEmpty()) {
            return Optional.empty();
        }

        final var name = tooltip.getFirst();
        final var title = (item.count() > 1 ? item.count() + " × " : "") + name.getString();

        final var details = new ArrayList<>(tooltip.subList(1, tooltip.size()));
        final var display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        if (stack.isDamaged() && display.shows(DataComponents.DAMAGE)) {
            // Vanilla only shows this with advanced tooltips (F3+H)
            final var max = stack.getMaxDamage();
            details.add(Component.translatable("item.durability", max - stack.getDamageValue(), max));
        }

        final var description = details.stream()
            .map(DiscordMarkdown::of)
            .collect(Collectors.joining("\n"))
            .strip();

        return Optional.of(new EmbedBuilder()
            .setTitle(TextComponents.truncate(title, MessageEmbed.TITLE_MAX_LENGTH))
            .setColor(colour(name))
            .setDescription(TextComponents.truncate(description, MessageEmbed.DESCRIPTION_MAX_LENGTH))
            // What the item is, even if it's been renamed. Footers aren't Markdown, so it's as it is
            .setFooter(TextComponents.truncate(stack.getItemName().getString(), MessageEmbed.TEXT_MAX_LENGTH))
            .build());
    }

    /**
     * Gets the first colour used in the given component, or falls back to white if none is found.
     * For items, this should handle custom formatting, and if not, item rarity as that's coloured
     * by Vanilla.
     */
    private static int colour(Component comp) {
        return comp.visit(
                (style, text) -> text.isEmpty()
                    ? Optional.empty()
                    : Optional.ofNullable(style.getColor()),
                Style.EMPTY
            )
            .orElse(TextColor.fromLegacyFormat(ChatFormatting.WHITE))
            .getValue();
    }
}
