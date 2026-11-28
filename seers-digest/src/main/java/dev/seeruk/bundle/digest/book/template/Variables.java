package dev.seeruk.bundle.digest.book.template;

import dev.seeruk.bundle.digest.book.template.Value.Amount;
import dev.seeruk.bundle.digest.book.template.Value.Text;
import dev.seeruk.bundle.text.TextTemplate;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The values a book's templates can show, by name, e.g. {@code miner.total}. Each list item adds
 * its own on top of these (see {@link #with}).
 */
public final class Variables {
    private final Map<String, Value> values;

    public Variables(Map<String, Value> values) {
        this.values = Map.copyOf(values);
    }

    /**
     * These, with the given values added, replacing any with the same names.
     */
    public Variables with(Map<String, Value> added) {
        final var values = new HashMap<>(this.values);
        values.putAll(added);
        return new Variables(values);
    }

    /**
     * The template filled in, if every one of its placeholders reads well (see {@link #fill}).
     */
    public Optional<Component> formatIfReadable(TextTemplate template) {
        final var filled = filled(template);
        return filled.size() == template.placeholders().stream().distinct().count()
            ? Optional.of(template.format(filled))
            : Optional.empty();
    }

    /**
     * The template filled in, leaving any placeholder that doesn't read well (see {@link #fill})
     * as it's written, so a mistake shows where it is.
     */
    public Component format(TextTemplate template) {
        return template.format(filled(template));
    }

    // What each of the template's placeholders that reads well shows, by its placeholder
    private Map<String, Component> filled(TextTemplate template) {
        final var filled = new HashMap<String, Component>();
        template.placeholders().forEach(placeholder -> fill(placeholder).ifPresent(value -> filled.put(placeholder, value)));
        return filled;
    }

    /**
     * What a placeholder shows, e.g. {@code miner.total|plural:block}, or nothing if it doesn't
     * read well: it names no value, has a filter that can't be used, or shows a number that's
     * more than 0 as 0.
     */
    public Optional<Component> fill(String placeholder) {
        final var parts = placeholder.split("\\|", -1);
        var value = Optional.ofNullable(values.get(parts[0].strip()));
        for (var index = 1; index < parts.length && value.isPresent(); index++) {
            final var filter = parts[index];
            value = value.flatMap(found -> Filters.apply(found, filter));
        }

        return value.flatMap(found -> switch (found) {
            case Text text -> Optional.of(text.text());
            case Amount amount -> amount.readsWell() ? Optional.of(Component.literal(amount.shown())) : Optional.empty();
        });
    }
}
