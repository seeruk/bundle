package dev.seeruk.bundle.discord;

import com.mojang.serialization.Codec;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A formattable template that supports placeholders in the format {@code ${placeholder}}. Values
 * are not handled with special care here, they're just put in as-is.
 *
 * @param template The unformatted template.
 */
public record DiscordTemplate(String template) {
    public static final Codec<DiscordTemplate> CODEC = Codec.STRING.xmap(DiscordTemplate::new, DiscordTemplate::template);

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([a-z_]+)}");

    /**
     * Fills in the placeholders, e.g. {@code Map.of("message", text)} for {@code ${message}}. Any
     * placeholder without a value is left as it was written, so mistakes are easy to spot. Values
     * are only filled in once, so placeholders in them (e.g. typed by a player) are left alone.
     */
    public String format(Map<String, String> values) {
        return PLACEHOLDER.matcher(template).replaceAll(placeholder ->
            Matcher.quoteReplacement(values.getOrDefault(placeholder.group(1), placeholder.group())));
    }
}
