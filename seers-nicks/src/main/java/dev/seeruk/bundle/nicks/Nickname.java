package dev.seeruk.bundle.nicks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
     * @param trusted   Whether an admin is setting it, who can use other players' usernames.
     * @param usernames Whether some text is another player's username.
     */
    public static Optional<NickEditResult.Failure> validate(Component name, boolean trusted, Predicate<String> usernames) {
        final var plain = name.getString();

        if (plain.isBlank()) {
            return Optional.of(new NickEditResult.BlankName());
        }

        if (plain.length() > MAX_LENGTH) {
            return Optional.of(new NickEditResult.MaxLengthExceeded(MAX_LENGTH, name));
        }

        if (!trusted && usernames.test(plain)) {
            return Optional.of(new NickEditResult.UsernameTaken(plain));
        }

        return Optional.empty();
    }
}
