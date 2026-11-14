package dev.seeruk.bundle.itemtext;

import dev.seeruk.bundle.itemtext.api.ItemNameEvents;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * NameEditor is a service that orchestrates changes to item names. Modifications to names are much
 * simpler than modifications to item lore, and as such don't need to reach out to separate
 * utilities.
 */
public final class NameEditor {
    private NameEditor() {}

    public static Optional<Component> customName(ItemStack stack) {
        return Optional.ofNullable(stack.get(DataComponents.CUSTOM_NAME));
    }

    public static NameEditResult set(Player player, ItemStack stack, String input) {
        final var original = customName(stack);
        final var parsed = TextParsers.parseAsUntrusted(input);
        final var plain = parsed.getString();

        if (plain.isBlank()) {
            return new NameEditResult.BlankName();
        }

        if (plain.length() > AnvilMenu.MAX_NAME_LENGTH) {
            return new NameEditResult.MaxLengthExceeded(AnvilMenu.MAX_NAME_LENGTH, parsed);
        }

        final var edited = new NameEditResult.Edited(normalizeName(original), Optional.of(parsed));
        if (original.map(parsed::equals).orElse(false)) {
            // We specifically don't go through the normal save route here, as if the name hasn't
            // changed, we don't want to deny it, but we also don't want to do things like fire
            // events - we can just ignore it silently.
            return edited;
        }

        return save(player, stack, edited);
    }

    public static NameEditResult reset(Player player, ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_NAME)) {
            return new NameEditResult.NoCustomName();
        }

        return save(player, stack, new NameEditResult.Edited(normalizeName(customName(stack)), Optional.empty()));
    }

    private static NameEditResult.Edited save(Player player, ItemStack stack, NameEditResult.Edited result) {
        // Setting `null` will properly remove the custom name.
        stack.set(DataComponents.CUSTOM_NAME, result.edited().orElse(null));
        ItemNameEvents.CHANGED.invoker().onNameChanged(player, stack, result.original(), result.edited());

        return result;
    }

    private static Optional<Component> normalizeName(Optional<Component> input) {
        // Other mods could be sloppier with names than we're being here, so let's normalize the
        // given name so we either get something that has plaintext length, or an empty optional.
        return input.filter(name -> !name.getString().isBlank());
    }
}
