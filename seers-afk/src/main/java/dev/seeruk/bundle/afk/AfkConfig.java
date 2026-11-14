package dev.seeruk.bundle.afk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.text.TextTemplate;

/**
 * @param timeoutSeconds How long a player can go without doing anything before they're AFK.
 * @param messages       What everyone's told when a player goes AFK, or comes back.
 */
public record AfkConfig(int timeoutSeconds, Messages messages) {
    public static final Codec<AfkConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(1, Integer.MAX_VALUE).fieldOf("timeout_seconds").forGetter(AfkConfig::timeoutSeconds),
        Messages.CODEC.fieldOf("messages").forGetter(AfkConfig::messages)
    ).apply(instance, AfkConfig::new));

    public long timeoutMillis() {
        return timeoutSeconds * 1000L;
    }

    /**
     * Each message can use {@code ${player}} for the player's display name.
     *
     * @param afk  Shown when a player goes AFK.
     * @param back Shown when a player comes back.
     */
    public record Messages(TextTemplate afk, TextTemplate back) {
        public static final Codec<Messages> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TextTemplate.CODEC.fieldOf("afk").forGetter(Messages::afk),
            TextTemplate.CODEC.fieldOf("back").forGetter(Messages::back)
        ).apply(instance, Messages::new));
    }
}
