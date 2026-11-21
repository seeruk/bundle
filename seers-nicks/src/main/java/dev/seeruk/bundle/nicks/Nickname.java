package dev.seeruk.bundle.nicks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.text.TextComponents;
import dev.seeruk.bundle.text.TextParsers;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * A nickname exactly as it was typed, so it can be formatted again if the formatting rules change.
 *
 * @param input   The nickname as it was typed, including any formatting tags.
 * @param trusted Whether it was set in a trusted context, where any formatting tags are allowed.
 */
public record Nickname(String input, boolean trusted) {
    public static final String PREFIX = "≈";
    public static final int MAX_LENGTH = 15;

    public static final Codec<Nickname> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("input").forGetter(Nickname::input),
        Codec.BOOL.fieldOf("trusted").forGetter(Nickname::trusted)
    ).apply(instance, Nickname::new));

    public Component format() {
        return trusted ? TextParsers.parseAsTrusted(input) : TextParsers.parseAsColours(input);
    }

    /**
     * @param name      The formatted nickname.
     * @param trusted   Whether an admin is setting it, who can use other players' usernames and
     *                  nicknames.
     * @param usernames Whether some text is another player's username.
     * @param nicknames Whether some text is another player's nickname.
     */
    public static Optional<NickEditResult.Failure> validate(
        Component name,
        boolean trusted,
        Predicate<String> usernames,
        Predicate<String> nicknames
    ) {
        final var text = name.getString();

        if (text.isBlank()) {
            return Optional.of(new NickEditResult.BlankName());
        }

        if (!text.equals(text.strip())) {
            return Optional.of(new NickEditResult.SurroundingWhitespace());
        }

        if (TextComponents.length(text) > MAX_LENGTH) {
            return Optional.of(new NickEditResult.MaxLengthExceeded(MAX_LENGTH, name));
        }

        if (!trusted && usernames.test(text)) {
            return Optional.of(new NickEditResult.UsernameTaken(text));
        }

        if (!trusted && nicknames.test(text)) {
            return Optional.of(new NickEditResult.NicknameTaken(text));
        }

        return Optional.empty();
    }
}
