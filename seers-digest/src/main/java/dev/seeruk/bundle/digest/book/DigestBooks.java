package dev.seeruk.bundle.digest.book;

import dev.seeruk.bundle.core.names.PlayerNames;
import dev.seeruk.bundle.digest.DigestConfig;
import dev.seeruk.bundle.digest.api.Issue;
import dev.seeruk.bundle.text.ReadableColours;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

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
    public static ItemStack create(MinecraftServer server, DigestConfig config, BookConfig book, Issue issue) {
        final var stack = new ItemStack(Items.WRITTEN_BOOK);
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
            Filterable.passThrough(book.title()),
            book.author(),
            0,
            layOut(server, config, book, issue).pages().stream()
                .map(page -> ReadableColours.onLight(ReadableColours.BOOK_PAGE, page))
                .map(Filterable::passThrough)
                .toList(),
            true
        ));
        return stack;
    }

    /**
     * Warns if the issue's book is too long for one book, so some of its sections are left out.
     */
    public static void warnIfTooLong(MinecraftServer server, DigestConfig config, BookConfig book, Issue issue) {
        final var leftOut = layOut(server, config, book, issue).leftOut();
        if (!leftOut.isEmpty()) {
            LOGGER.warn(
                "Issue #{} is too long for one book, so its last {} sections are left out. Lower other sections' max_pages in book.yml to make room for them",
                issue.number(),
                leftOut.size()
            );
        }
    }

    private static BookLayout.LaidOut layOut(MinecraftServer server, DigestConfig config, BookConfig book, Issue issue) {
        final var written = BookWriter.write(issue, config, book, id -> PlayerNames.displayName(server, id));
        return BookLayout.layOut(written.cover(), written.centredCover(), written.sections(), written.wording());
    }
}
