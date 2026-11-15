package dev.seeruk.bundle.itemtext;

import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.stream.IntStream;

/**
 * What to tell a player after they've edited their item's lore.
 */
public final class LoreEditMessages {
    private LoreEditMessages() {}

    /**
     * The message for the result of editing the lore, with a preview of the lore if it was edited.
     */
    public static Component of(LoreEditResult result) {
        return switch (result) {
            case LoreEditResult.Edited edited -> Component.empty()
                .append(TextParsers.parseAsTrusted("<green>Lore updated:</green>\n"))
                .append(preview(edited.edited()));
            case LoreEditResult.MaxLineLengthExceeded failure -> TextParsers.parseAsTrusted(
                "<red>A line of lore cannot be longer than %d characters (yours is %d)</red>".formatted(
                    failure.maxLineLength(),
                    failure.line().getString().length()
                )
            );
            case LoreEditResult.MaxLinesExceeded failure -> TextParsers.parseAsTrusted(
                "<red>Lore can't be more than %d lines (yours is %d)</red>".formatted(
                    failure.maxLines(),
                    failure.resultingLines()
                )
            );
        };
    }

    /**
     * Constructs a preview of the lore lines as a single component, numbering each line.
     */
    private static Component preview(List<Component> lines) {
        final var styled = new ItemLore(lines).styledLines();
        if (styled.isEmpty()) {
            return Component.literal("No lore").withStyle(ChatFormatting.GRAY);
        }

        final var numbered = IntStream.range(0, styled.size())
            .mapToObj(i ->
                Component.empty()
                    .append(Component.literal((i + 1) + ": ").withStyle(ChatFormatting.GRAY))
                    .append(styled.get(i))
            )
            .toList();

        return ComponentUtils.formatList(numbered, Component.literal("\n"));
    }
}
