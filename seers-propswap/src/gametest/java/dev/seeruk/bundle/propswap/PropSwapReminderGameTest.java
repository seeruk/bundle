package dev.seeruk.bundle.propswap;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static dev.seeruk.bundle.propswap.PropSwapFixtures.player;

public class PropSwapReminderGameTest {
    @GameTest
    public void backsOffForOtherActionBarMessages(GameTestHelper helper) {
        final var player = player(helper, Items.IRON_AXE);
        final var now = helper.getLevel().getServer().getTickCount();
        helper.assertTrue(PropSwapReminder.shouldRemind(player, now), "No reminder while holding a tool");

        // Normal chat doesn't use the action bar, so the reminder doesn't care about it
        PropSwapReminder.packetSent(player, new ClientboundSystemChatPacket(Component.literal("hi"), false));
        helper.assertTrue(PropSwapReminder.shouldRemind(player, now), "Backed off for chat");

        PropSwapReminder.packetSent(player, new ClientboundSystemChatPacket(Component.literal("hi"), true));
        helper.assertTrue(!PropSwapReminder.shouldRemind(player, now), "Didn't back off");
        helper.assertTrue(
            PropSwapReminder.shouldRemind(player, now + PropSwapReminder.BACK_OFF_TICKS),
            "Never came back"
        );
        helper.succeed();
    }

    @GameTest
    public void backsOffForTitleActionBarMessages(GameTestHelper helper) {
        // `/title <player> actionbar ...` uses its own packet, rather than an overlay chat message
        final var player = player(helper, Items.IRON_AXE);
        final var now = helper.getLevel().getServer().getTickCount();

        PropSwapReminder.packetSent(player, new ClientboundSetActionBarTextPacket(Component.literal("hi")));

        helper.assertTrue(!PropSwapReminder.shouldRemind(player, now), "Didn't back off");
        helper.succeed();
    }

    @GameTest
    public void doesNotBackOffForItsOwnMessage(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper, Items.IRON_AXE);
        final var now = server.getTickCount();

        // Sends the reminder for real, so it passes through the packet mixin
        PropSwapReminder.remind(server);

        helper.assertTrue(!PropSwapReminder.shouldRemind(player, now), "Reminded again before refreshing");
        helper.assertTrue(
            PropSwapReminder.shouldRemind(player, now + PropSwapReminder.REFRESH_TICKS),
            "Backed off for itself"
        );
        helper.succeed();
    }

    @GameTest
    public void recognisesCopiesOfItsOwnMessage(GameTestHelper helper) {
        // Other mods might rebuild overlay messages before they're sent
        final var player = player(helper, Items.IRON_AXE);
        final var now = helper.getLevel().getServer().getTickCount();
        final var copy = Component.literal("PropSwap enabled").withStyle(ChatFormatting.GRAY);

        PropSwapReminder.packetSent(player, new ClientboundSystemChatPacket(copy, true));

        helper.assertTrue(PropSwapReminder.shouldRemind(player, now), "Backed off for a copy of itself");
        helper.succeed();
    }

    @GameTest
    public void hidingLeavesNothingToWaitFor(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var player = player(helper, Items.IRON_AXE);
        final var now = server.getTickCount();
        PropSwapReminder.remind(server);

        // Hiding sends its own action bar message, which shouldn't hold up the next reminder either
        PropSwapReminder.hide(player);

        helper.assertTrue(PropSwapReminder.shouldRemind(player, now), "Reminder was held up after hiding");
        helper.succeed();
    }

    @GameTest
    public void onlyRemindsWhileEnabledAndHoldingATool(GameTestHelper helper) {
        final var player = player(helper, Items.STICK);
        final var now = helper.getLevel().getServer().getTickCount();
        helper.assertTrue(!PropSwapReminder.shouldRemind(player, now), "Reminded without a tool");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        PropSwapMode.disable(player);

        helper.assertTrue(!PropSwapReminder.shouldRemind(player, now), "Reminded while disabled");
        helper.succeed();
    }
}
