package dev.seeruk.bundle.digest.book.template;

import dev.seeruk.bundle.digest.book.template.Value.Amount;
import dev.seeruk.bundle.digest.book.template.Value.Display;
import dev.seeruk.bundle.digest.book.template.Value.Text;

import java.util.Optional;

/**
 * The filters a book's templates can change a value with, written after it, e.g.
 * {@code ${played.total|minutes|plural:minute}}. Each gives nothing if it can't be used on the
 * value, so whatever it's in doesn't read well.
 */
public final class Filters {
    private Filters() {}

    /**
     * The value with the filter applied, e.g. {@code round:2}, or nothing if it's not a filter, or
     * can't be used on the value.
     */
    public static Optional<Value> apply(Value value, String filter) {
        final var parts = filter.strip().split(":", -1);
        return switch (parts[0]) {
            case "plural" -> plural(value, parts);
            case "round" -> round(value, parts);
            case "blocks" -> display(value, Display.BLOCKS);
            case "km" -> display(value, Display.KM);
            case "minutes" -> display(value, Display.MINUTES);
            case "hours" -> display(value, Display.HOURS);
            case "days" -> display(value, Display.DAYS);
            default -> Optional.empty();
        };
    }

    // e.g. plural:player for "1 player" or "3 players", and plural:person:people for "3 people",
    // by the number as it's shown, so 1.04 hours to 1 place is "1 hour"
    private static Optional<Value> plural(Value value, String[] parts) {
        if (!(value instanceof Amount amount) || parts.length < 2 || parts.length > 3 || parts[1].isEmpty() || !amount.readsWell()) {
            return Optional.empty();
        }

        final var plural = parts.length == 3 ? parts[2] : parts[1] + "s";
        final var one = Math.abs(amount.rounded()) == 1;
        return Optional.of(Text.of(amount.shown() + " " + (one ? parts[1] : plural)));
    }

    private static Optional<Value> round(Value value, String[] parts) {
        if (!(value instanceof Amount amount) || parts.length != 2) {
            return Optional.empty();
        }

        try {
            final var decimals = Integer.parseInt(parts[1]);
            return decimals < 0 || decimals > 6
                ? Optional.empty()
                : Optional.of(new Amount(amount.raw(), amount.unit(), amount.display(), decimals));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Value> display(Value value, Display display) {
        return value instanceof Amount amount && amount.unit() == display.unit
            ? Optional.of(new Amount(amount.raw(), amount.unit(), display, display.decimals))
            : Optional.empty();
    }
}
