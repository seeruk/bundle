package dev.seeruk.bundle.discord.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.seeruk.bundle.discord.WebhookNames;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

/**
 * How the server looks in Discord, i.e. the name and avatar of the bot's webhook, which event
 * messages are sent as.
 *
 * @param name   The webhook's name.
 * @param avatar The webhook's avatar, as a URL, or a file (relative to the server's folder). If
 *               it's blank, the server's icon is used.
 */
public record ServerConfig(String name, String avatar) {
    private static final Codec<String> NAME_CODEC = Codec.STRING.validate(name -> {
        if (name.isBlank() || name.length() > WebhookNames.MAX_LENGTH) {
            return DataResult.error(() -> "The server's name in Discord must be 1 to %d characters: %s".formatted(
                WebhookNames.MAX_LENGTH,
                name
            ));
        }

        return WebhookNames.hasReservedWord(name)
            ? DataResult.error(() -> "Discord doesn't allow \"Discord\" or \"Clyde\" in the server's name: " + name)
            : DataResult.success(name);
    });

    private static final Codec<String> AVATAR_CODEC = Codec.STRING.validate(avatar -> {
        if (!isUrl(avatar)) {
            return DataResult.success(avatar);
        }
        try {
            URI.create(avatar);
            return DataResult.success(avatar);
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Not a valid URL for the server's avatar in Discord: " + avatar);
        }
    });

    public static final Codec<ServerConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        NAME_CODEC.fieldOf("name").forGetter(ServerConfig::name),
        AVATAR_CODEC.fieldOf("avatar").forGetter(ServerConfig::avatar)
    ).apply(instance, ServerConfig::new));

    /**
     * Returns where the webhook's avatar is, if anywhere.
     *
     * @param serverDirectory The server's folder, which a file's path is relative to.
     * @param serverIcon      The server's icon, if it has one, used if the avatar is blank.
     */
    public Optional<URI> avatarLocation(Path serverDirectory, Optional<Path> serverIcon) {
        if (avatar.isBlank()) {
            return serverIcon.map(Path::toUri);
        }
        if (isUrl(avatar)) {
            return Optional.of(URI.create(avatar));
        }
        return Optional.of(serverDirectory.resolve(avatar).toUri());
    }

    private static boolean isUrl(String avatar) {
        return avatar.startsWith("https://") || avatar.startsWith("http://");
    }
}
