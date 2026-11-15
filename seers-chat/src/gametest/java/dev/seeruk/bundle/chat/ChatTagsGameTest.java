package dev.seeruk.bundle.chat;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static dev.seeruk.bundle.chat.ChatFixtures.chat;
import static dev.seeruk.bundle.chat.ChatFixtures.onlySentBy;
import static dev.seeruk.bundle.chat.ChatFixtures.styles;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;

public class ChatTagsGameTest {
    @GameTest
    public void slotTagsLinkTheirSlotsItem(GameTestHelper helper) {
        final var player = player(helper);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));

        chat(player, "<head> <item head> <offhand>");

        helper.succeedWhen(() -> {
            final var content = content(helper, player);
            helper.assertValueEqual(content.getString(), "[Diamond Helmet] [Diamond Helmet] [Shield]", "message text");
            helper.assertValueEqual(itemsShown(content), List.of(Items.DIAMOND_HELMET, Items.SHIELD), "items shown");
        });
    }

    @GameTest
    public void emptyOrUnknownSlotsAreLeftAsWritten(GameTestHelper helper) {
        final var player = player(helper);

        chat(player, "<chest> <item chest> <item nothing>");

        helper.succeedWhen(() -> helper.assertValueEqual(
            content(helper, player).getString(),
            "<chest> <item chest> <item nothing>",
            "message text"
        ));
    }

    @GameTest
    public void posShowsThePlayersCoordinates(GameTestHelper helper) {
        final var player = player(helper);
        player.teleportTo(10.5, 64, -20.5);

        chat(player, "<pos>");

        helper.succeedWhen(() -> {
            final var content = content(helper, player);
            helper.assertValueEqual(content.getString(), "[10, 64, -21]", "message text");
            assertPosition(helper, content, "Overworld", "10 64 -21");
        });
    }

    @GameTest
    public void dimShowsTheDimensionsName(GameTestHelper helper) {
        final var player = player(helper);
        helper.assertValueEqual(player.level().dimension(), Level.OVERWORLD, "dimension");

        chat(player, "<dim>");

        helper.succeedWhen(() -> helper.assertValueEqual(content(helper, player).getString(), "Overworld", "message text"));
    }

    @GameTest
    public void deathShowsWhereThePlayerLastDied(GameTestHelper helper) {
        final var player = player(helper);
        player.setLastDeathLocation(Optional.of(GlobalPos.of(Level.NETHER, new BlockPos(1, 2, 3))));

        chat(player, "<death>");

        helper.succeedWhen(() -> {
            final var content = content(helper, player);
            helper.assertValueEqual(content.getString(), "[1, 2, 3]", "message text");
            assertPosition(helper, content, "The Nether", "1 2 3");
        });
    }

    @GameTest
    public void deathIsLeftAsWrittenIfThePlayerHasNotDied(GameTestHelper helper) {
        final var player = player(helper);

        chat(player, "<death>");

        helper.succeedWhen(() -> helper.assertValueEqual(content(helper, player).getString(), "<death>", "message text"));
    }

    @GameTest
    public void biomeShowsTheBiomesName(GameTestHelper helper) {
        final var player = player(helper);
        final var biome = player.level().getBiome(player.blockPosition()).unwrapKey().orElseThrow().identifier();

        chat(player, "<biome>");

        helper.succeedWhen(() -> {
            final var keys = translationKeys(content(helper, player));
            helper.assertValueEqual(keys, List.of("biome." + biome.getNamespace() + "." + biome.getPath()), "translation keys");
        });
    }

    @GameTest
    public void healthShowsTheirHealthRoundedUp(GameTestHelper helper) {
        final var player = player(helper);
        player.setHealth(13.5f);

        chat(player, "<health>");

        helper.succeedWhen(() -> helper.assertValueEqual(content(helper, player).getString(), "❤ 14/20", "message text"));
    }

    @GameTest
    public void levelShowsTheirExperienceLevel(GameTestHelper helper) {
        final var player = player(helper);
        player.setExperienceLevels(30);

        chat(player, "<level>");

        helper.succeedWhen(() -> helper.assertValueEqual(content(helper, player).getString(), "Level 30", "message text"));
    }

    private static Component content(GameTestHelper helper, ServerPlayer player) {
        return onlySentBy(helper, player).message().decoratedContent();
    }

    /**
     * The different items shown when hovering over the message, in order.
     */
    private static List<Item> itemsShown(Component content) {
        return styles(content).stream()
            .map(Style::getHoverEvent)
            .filter(event -> event instanceof HoverEvent.ShowItem)
            .map(event -> ((HoverEvent.ShowItem) event).item().item().value())
            .distinct()
            .toList();
    }

    private static List<String> translationKeys(Component component) {
        final var keys = new ArrayList<String>();
        if (component.getContents() instanceof TranslatableContents translatable) {
            keys.add(translatable.getKey());
        }
        component.getSiblings().forEach(sibling -> keys.addAll(translationKeys(sibling)));
        return keys;
    }

    private static void assertPosition(GameTestHelper helper, Component content, String dimension, String copied) {
        final var style = styles(content).getFirst();
        final var hover = (HoverEvent.ShowText) Objects.requireNonNull(style.getHoverEvent());
        final var click = (ClickEvent.CopyToClipboard) Objects.requireNonNull(style.getClickEvent());

        helper.assertValueEqual(style.getColor(), TextColor.fromLegacyFormat(ChatFormatting.AQUA), "colour");
        helper.assertValueEqual(hover.value().getString(), dimension + "\nClick to copy", "hover text");
        helper.assertValueEqual(click.value(), copied, "copied text");
    }
}
