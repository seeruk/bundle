package dev.seeruk.bundle.digest.book;

import dev.seeruk.bundle.core.names.PlayerNames;
import dev.seeruk.bundle.digest.DigestConfig;
import dev.seeruk.bundle.digest.api.Issue;
import dev.seeruk.bundle.text.ReadableColours;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.Map;

import static dev.seeruk.bundle.digest.SeersDigest.LOGGER;

/**
 * Issues' books, made each time they're needed, so changing how books look changes old issues too.
 */
public final class DigestBooks {
    private DigestBooks() {}

    /**
     * A written book of the issue, with players shown by the names they have now, and colours too
     * light for the page darkened.
     */
    public static ItemStack create(MinecraftServer server, DigestConfig config, Issue issue) {
        final var sections = PlainSections.of(issue, config, id -> PlayerNames.displayName(server, id));
        final var book = BookLayout.layOut(cover(config, issue), sections);
        if (!book.leftOut().isEmpty()) {
            LOGGER.warn("Issue #{} is too long for one book, so its last {} sections were left out", issue.number(), book.leftOut().size());
        }

        final var stack = new ItemStack(Items.WRITTEN_BOOK);
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
            Filterable.passThrough(config.book().title()),
            config.book().author(),
            0,
            book.pages().stream()
                .map(page -> ReadableColours.onLight(ReadableColours.BOOK_PAGE, page))
                .map(Filterable::passThrough)
                .toList(),
            true
        ));
        return stack;
    }

    private static Component cover(DigestConfig config, Issue issue) {
        final var dates = config.book().dates().withZone(config.timezone());
        return config.book().cover().format(Map.of(
            "title", Component.literal(config.book().title()),
            "number", Component.literal(Integer.toString(issue.number())),
            "from", Component.literal(dates.format(issue.started())),
            "to", Component.literal(dates.format(issue.ended())),
            "period", Component.literal(switch (config.schedule().every()) {
                case DAY -> "Daily";
                case WEEK -> "Weekly";
                case MONTH -> "Monthly";
            })
        ));
    }
}
