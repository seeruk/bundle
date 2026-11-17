package dev.seeruk.bundle.chat;

import dev.seeruk.bundle.text.TextColours;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.DynamicTextNode;
import eu.pb4.placeholders.api.parsers.tag.TextTag;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tags players can use in their messages, filled in for the player who wrote the message, e.g.
 * {@code <item>} or {@code <pos>}. Tags that have nothing to show (e.g. an empty hand) are left as
 * they were written.
 */
public final class ChatTags {
    /**
     * Fills in a tag, given it as it was written (without the angle brackets), for the player
     * who wrote it.
     */
    static final ParserContext.Key<Function<String, Component>> KEY = DynamicTextNode.key(
        SeersChat.id("tags").toString());

    // The slots that can be linked, which are also tags of their own (e.g. <head>)
    private static final List<EquipmentSlot> SLOTS = List.of(
        EquipmentSlot.MAINHAND,
        EquipmentSlot.OFFHAND,
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    );

    private static final Map<ResourceKey<Level>, String> DIMENSION_NAMES = Map.of(
        Level.OVERWORLD, "Overworld",
        Level.NETHER, "The Nether",
        Level.END, "The End"
    );

    static final TextTag[] TAGS = Stream.concat(
        Stream.of(
            // <item> links the main hand item, and <item head> etc. link another slot
            TextTag.self(
                "item", "other", true, args ->
                    DynamicTextNode.of(args.input().isBlank() ? "item" : "item " + args.input().trim(), KEY)
            ),
            tag("pos"),
            tag("dim"),
            tag("death"),
            tag("biome"),
            tag("health"),
            tag("level")
        ),
        SLOTS.stream().map(slot -> tag(slot.getName()))
    ).toArray(TextTag[]::new);

    private ChatTags() {}

    /**
     * Fills in a tag for the player.
     *
     * @param player The player who wrote the tag.
     * @param tag    The tag as it was written, without the angle brackets, e.g. {@code item head}.
     */
    static Component resolve(ServerPlayer player, String tag) {
        final var parts = tag.split(" ", 2);
        final Optional<Component> resolved = switch (parts[0]) {
            case "item" -> item(player, parts.length > 1 ? parts[1] : EquipmentSlot.MAINHAND.getName());
            case "pos" -> Optional.of(position(player.level().dimension(), player.blockPosition()));
            case "dim" -> Optional.of(Component.literal(dimensionName(player.level().dimension())));
            case "death" -> player.getLastDeathLocation().map(death -> position(death.dimension(), death.pos()));
            case "biome" -> player.level().getBiome(player.blockPosition()).unwrapKey()
                .map(biome -> Component.translatable(biome.identifier().toLanguageKey("biome")));
            case "health" -> Optional.of(health(player));
            case "level" -> Optional.of(Component.literal("Level " + player.experienceLevel)
                .withStyle(ChatFormatting.GREEN));
            default -> item(player, parts[0]);
        };

        return resolved.orElseGet(() -> Component.literal("<" + tag + ">"));
    }

    /**
     * Create a tag with the given name, that resolves to a {@link DynamicTextNode} using our
     * {@link #KEY}. This is later resolved by {@link #resolve(ServerPlayer, String)} to fill in the
     * resulting text dynamically.
     *
     * @param name The name of the tag to create
     */
    private static TextTag tag(String name) {
        return TextTag.self(name, "other", true, args -> DynamicTextNode.of(name, KEY));
    }

    private static Optional<Component> item(ServerPlayer player, String slotName) {
        return SLOTS.stream()
            .filter(slot -> slot.getName().equals(slotName))
            .findFirst()
            .map(player::getItemBySlot)
            .filter(stack -> !stack.isEmpty())
            .map(ItemStack::getDisplayName);
    }

    /**
     * Returns the given block position as {@code [x, y, z]}, showing the dimension when hovered
     * over, and copying the coordinates when clicked.
     */
    private static Component position(ResourceKey<Level> dimension, BlockPos pos) {
        final var hover = Component.literal(dimensionName(dimension))
            .append(Component.literal("\nClick to copy").withStyle(ChatFormatting.GRAY));

        return Component.literal("[%d, %d, %d]".formatted(pos.getX(), pos.getY(), pos.getZ()))
            .withStyle(style -> style
                .withColor(TextColours.CLICKABLE)
                .withHoverEvent(new HoverEvent.ShowText(hover))
                .withClickEvent(new ClickEvent.CopyToClipboard(
                    "%d %d %d".formatted(pos.getX(), pos.getY(), pos.getZ()))));
    }

    /**
     * Dimensions are surprisingly not named in Vanilla, so other dimensions' names are "generated"
     * based on their ID, e.g. {@code example:mining_world} is "Mining World".
     */
    private static String dimensionName(ResourceKey<Level> dimension) {
        return DIMENSION_NAMES.getOrDefault(
            dimension, Arrays.stream(dimension.identifier().getPath().split("_"))
                .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "))
        );
    }

    private static Component health(ServerPlayer player) {
        // Rounded up, similar to how hearts are shown
        final var health = (int) Math.ceil(player.getHealth());
        final var max = (int) Math.ceil(player.getMaxHealth());

        return Component.empty()
            .append(Component.literal("❤ ").withStyle(ChatFormatting.RED))
            .append(health + "/" + max);
    }
}
