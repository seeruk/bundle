package dev.seeruk.bundle.discord.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Configuration for the available Discord slash commands provided by this mod, and whether their
 * replies are visible to everyone or not.
 *
 * @param publicReplies Whether the bots slash command replies are visible to everyone.
 * @param players       {@code /players} (and {@code /playerlist}), which shows who's online.
 * @param status        {@code /status}, which shows the status of the server.
 */
public record CommandsConfig(boolean publicReplies, Command players, Command status) {
    public static final Codec<CommandsConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("public_replies").forGetter(CommandsConfig::publicReplies),
        Command.CODEC.fieldOf("players").forGetter(CommandsConfig::players),
        Command.CODEC.fieldOf("status").forGetter(CommandsConfig::status)
    ).apply(instance, CommandsConfig::new));

    /**
     * @param enabled Whether the command is available.
     */
    public record Command(boolean enabled) {
        public static final Codec<Command> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("enabled").forGetter(Command::enabled)
        ).apply(instance, Command::new));
    }
}
