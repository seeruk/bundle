package dev.seeruk.bundle.digest.book;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A titled part of a book, starting on a page of its own, made of entries: the things it's about,
 * e.g. one award each. An entry can have line breaks of its own, and lines too wide for a page,
 * which are wrapped to fit.
 *
 * @param title   Shown at the top of the section, and in the contents.
 * @param entries What the section says, in order.
 */
public record BookSection(Component title, List<Component> entries) {}
