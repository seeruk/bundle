package dev.seeruk.bundle.itemtext.dialog;

import dev.seeruk.bundle.itemtext.*;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A dialog for writing an item's whole lore in one go, as one piece of text.
 */
public final class LoreDialog {
    public static final Identifier SAVE_ACTION = SeersItemText.id("edit_lore");

    static final String INPUT = "lore";
    static final int MAX_INPUT_LENGTH = 2048;

    private static final int WIDTH = 300;
    // Fits 15 lines of text, the same way Vanilla sizes a box for a number of lines
    private static final int HEIGHT = 9 * 15 + 8;

    private static final Component HINT = Component.literal(
        "Each line here is a line of lore, and long lines wrap. Formatting tags can span lines."
    ).withStyle(ChatFormatting.GRAY);

    private static final Component NOT_AS_WRITTEN = Component.literal(
        "This item's lore was changed via other means, so its formatting isn't shown here. Saving will update it."
    ).withStyle(ChatFormatting.YELLOW);

    private LoreDialog() {}

    /**
     * Opens this dialog for the given item, filled with the text its lore was last written with,
     * or its plain text if that's not available.
     *
     * @param player The player opening the dialog.
     * @param stack  The item that's being edited.
     */
    public static void open(ServerPlayer player, ItemStack stack) {
        final var input = LoreInput.get(stack);
        if (input.isPresent()) {
            open(player, input.get(), Optional.empty());
            return;
        }

        // If grabbing the previous raw input above didn't work, fall back to the plain text.
        final var lines = LoreEditor.lines(stack);
        final var plain = lines.stream()
            .map(Component::getString)
            .collect(Collectors.joining("\n"));

        open(player, plain, lines.isEmpty() ? Optional.empty() : Optional.of(NOT_AS_WRITTEN));
    }

    /**
     * @param player  The player to open the dialog for.
     * @param text    The text to fill the dialog with.
     * @param message Shown above the text, e.g. what went wrong when saving.
     */
    private static void open(ServerPlayer player, String text, Optional<Component> message) {
        player.openDialog(Holder.direct(create(text, message)));
    }

    /**
     * Saves the lore the player wrote to the item in their main hand, or reopens the dialog with
     * what went wrong, retaining what they wrote so they can try again.
     *
     * @param player  The player who pressed Save.
     * @param payload The dialog's values, sent by the player's client.
     */
    public static void save(ServerPlayer player, CompoundTag payload) {
        final var stack = EditableItem.inMainHand(player, player::sendSystemMessage);
        if (stack.isEmpty()) {
            return;
        }

        final var input = payload.getStringOr(INPUT, "");
        if (input.length() > MAX_INPUT_LENGTH) {
            open(
                player, input, Optional.of(TextParsers.parseAsTrusted(
                    "<red>Lore can't be longer than %d characters</red>".formatted(MAX_INPUT_LENGTH)
                ))
            );
            return;
        }

        final var result = LoreEditor.replace(player, stack.get(), input);
        if (result instanceof LoreEditResult.Edited) {
            player.sendSystemMessage(LoreEditMessages.of(result));
        } else {
            open(player, input, Optional.of(LoreEditMessages.of(result)));
        }
    }

    /**
     * Builds the dialog, component-by-component.
     *
     * @param text    The text to fill the dialog input with. Cut down to {@link #MAX_INPUT_LENGTH}
     *                if needed, as the client refuses longer text.
     * @param message Shown above the text, e.g. what went wrong when saving.
     */
    static Dialog create(String text, Optional<Component> message) {
        final var body = new ArrayList<DialogBody>();
        message.ifPresent(m -> body.add(new PlainMessage(m, WIDTH)));
        body.add(new PlainMessage(HINT, WIDTH));

        final var initial = truncate(text, MAX_INPUT_LENGTH);
        final var input = new Input(
            INPUT, new TextInput(
            WIDTH,
            Component.literal("Lore"),
            false,
            initial,
            MAX_INPUT_LENGTH,
            // A height, not a line limit, as the client counts wrapped lines towards a limit, and
            // won't fill the box with text over it
            Optional.of(new TextInput.MultilineOptions(Optional.empty(), Optional.of(HEIGHT)))
        )
        );

        return new ConfirmationDialog(
            new CommonDialogData(
                Component.literal("Edit Lore"),
                Optional.empty(),
                true,
                false,
                DialogAction.CLOSE,
                List.copyOf(body),
                List.of(input)
            ),
            new ActionButton(
                new CommonButtonData(Component.literal("Save"), Optional.empty(), CommonButtonData.DEFAULT_WIDTH),
                Optional.of(new CustomAll(SAVE_ACTION, Optional.empty()))
            ),
            new ActionButton(
                new CommonButtonData(CommonComponents.GUI_CANCEL, Optional.empty(), CommonButtonData.DEFAULT_WIDTH),
                Optional.empty()
            )
        );
    }

    /**
     * Cuts the text down to at most {@code maxLength} UTF-16 units, as the client measures it,
     * without cutting a character (e.g. an emoji) in half.
     */
    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        final var characters = BreakIterator.getCharacterInstance(Locale.ROOT);
        characters.setText(text);
        return text.substring(0, characters.preceding(maxLength + 1));
    }
}
