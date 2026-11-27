package dev.seeruk.bundle.digest.book;

import dev.seeruk.bundle.core.testing.TextChunks;
import dev.seeruk.bundle.digest.Archive;
import dev.seeruk.bundle.digest.api.Issue;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.TeamColor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import static dev.seeruk.bundle.core.testing.StatScores.addObjective;
import static dev.seeruk.bundle.core.testing.TestPlayers.player;
import static dev.seeruk.bundle.digest.DigestFixtures.config;

public class DigestBooksGameTest {
    private static final Instant STARTED = Instant.parse("2026-11-21T18:00:00Z");
    private static final Instant ENDED = Instant.parse("2026-11-22T18:00:00Z");

    // An issue where only the player was active, numbered after every issue so far
    private static Issue issue(GameTestHelper helper, ServerPlayer player) {
        final var server = helper.getLevel().getServer();
        return new Issue(
            Archive.nextIssueNumber(server),
            STARTED,
            ENDED,
            Map.of(),
            List.of(player.getUUID()),
            List.of(),
            Issue.Moments.EMPTY
        );
    }

    @GameTest
    public void bookHasTheConfiguredTitleAuthorAndCover(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "digest_books_cover");

        try {
            final var issue = issue(helper, player(helper));
            final var content = DigestBooks.create(server, config(objective), issue).get(DataComponents.WRITTEN_BOOK_CONTENT);

            helper.assertValueEqual(content.title().raw(), "Test Digest", "title");
            helper.assertValueEqual(content.author(), "Tests", "author");
            // The cover is wrapped to fit the page, dropping the spaces it's wrapped at
            helper.assertValueEqual(
                content.pages().getFirst().raw().getString().replace("\n", " "),
                "Test Digest Daily #%d: 21 Nov 2026 to 22 Nov 2026".formatted(issue.number()),
                "cover"
            );
            helper.assertValueEqual(
                content.pages().get(1).raw().getString().split("\n")[0],
                "Contents",
                "contents"
            );
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void bookShowsPlayersByTheirNames(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "digest_books_names");
        final var player = player(helper);

        try {
            final var base = issue(helper, player);
            final var issue = new Issue(
                base.number(),
                base.started(),
                base.ended(),
                base.groups(),
                base.activePlayers(),
                List.of(player.getUUID()),
                base.moments()
            );
            final var pages = DigestBooks.create(server, config(objective), issue)
                .get(DataComponents.WRITTEN_BOOK_CONTENT)
                .pages().stream()
                // Long names are wrapped onto the next line, dropping the space they're wrapped at
                .map(page -> page.raw().getString().replace("\n", " "))
                .toList();

            helper.assertTrue(
                pages.contains("Welcome  Welcome, " + player.getGameProfile().name() + "!"),
                "New player wasn't welcomed by name: " + pages
            );
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void bookDarkensColoursTooLightForThePage(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "digest_books_colours");
        final var team = server.getScoreboard().addPlayerTeam("digest_books_yellow");
        final var player = player(helper);
        team.setColor(Optional.of(TeamColor.YELLOW));
        server.getScoreboard().addPlayerToTeam(player.getGameProfile().name(), team);

        try {
            final var base = issue(helper, player);
            final var issue = new Issue(
                base.number(),
                base.started(),
                base.ended(),
                base.groups(),
                base.activePlayers(),
                List.of(player.getUUID()),
                base.moments()
            );
            final var colours = DigestBooks.create(server, config(objective), issue)
                .get(DataComponents.WRITTEN_BOOK_CONTENT)
                .pages().stream()
                .flatMap(page -> TextChunks.chunks(page.raw()).stream())
                .filter(chunk -> chunk.text().contains(player.getGameProfile().name()))
                .map(chunk -> chunk.style().getColor())
                .toList();

            helper.assertTrue(!colours.isEmpty(), "Player's name wasn't in the book");
            helper.assertTrue(colours.stream().allMatch(Objects::nonNull), "Player's name lost its team colour");
            helper.assertTrue(
                colours.stream().noneMatch(TextColor.fromLegacyFormat(ChatFormatting.YELLOW)::equals),
                "Player's name was still yellow"
            );
        } finally {
            server.getScoreboard().removePlayerTeam(team);
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }

    @GameTest
    public void bookLeavesOutSectionsThatDoNotFit(GameTestHelper helper) {
        final var server = helper.getLevel().getServer();
        final var objective = addObjective(server, "digest_books_long");
        final var player = player(helper);

        try {
            // A death each, far more than fit in 100 pages
            final var deaths = IntStream.range(0, 2000)
                .mapToObj(index -> new Issue.Death(player.getUUID(), "minecraft:fall", Optional.empty(), STARTED))
                .toList();
            final var base = issue(helper, player);
            final var issue = new Issue(
                base.number(),
                base.started(),
                base.ended(),
                base.groups(),
                base.activePlayers(),
                base.newPlayers(),
                new Issue.Moments(deaths, List.of(), List.of())
            );
            final var content = DigestBooks.create(server, config(objective), issue).get(DataComponents.WRITTEN_BOOK_CONTENT);

            final var contents = content.pages().get(1).raw().getString();
            helper.assertTrue(content.pages().size() <= BookLayout.MAX_PAGES, "Book has " + content.pages().size() + " pages");
            helper.assertTrue(contents.contains("Headlines"), "Headlines were left out: " + contents);
            helper.assertTrue(!contents.contains("Deaths"), "Deaths weren't left out: " + contents);
        } finally {
            server.getScoreboard().removeObjective(objective);
        }

        helper.succeed();
    }
}
