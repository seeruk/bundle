package dev.seeruk.bundle.digest.book;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * A titled part of a book, starting on a page of its own, made of entries: the things it's about,
 * e.g. one death each. An entry can have line breaks of its own, and lines too wide for a page,
 * which are wrapped to fit.
 *
 * @param title    Shown at the top of the section, and in the contents.
 * @param entries  What the section says, in order.
 * @param maxPages The most pages the section can take, or 0 for as many as it needs.
 * @param more     Given how many entries didn't fit in its pages, the entry that ends the section
 *                 instead, if any (e.g. "…and 12 more").
 */
public record BookSection(
    Component title,
    List<Component> entries,
    int maxPages,
    IntFunction<Optional<Component>> more
) {}
