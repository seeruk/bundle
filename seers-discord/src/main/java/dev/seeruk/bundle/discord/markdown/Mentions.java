package dev.seeruk.bundle.discord.markdown;

import java.util.Map;

/**
 * Context to be used to look up mentions from Discord by their IDs, returning their names.
 *
 * @param users    Users' names as shown in the bot's server, e.g. their nickname.
 * @param roles    Roles' names.
 * @param channels Channels' names.
 */
public record Mentions(Map<Long, String> users, Map<Long, String> roles, Map<Long, String> channels) {
    public static final Mentions NONE = new Mentions(Map.of(), Map.of(), Map.of());
}
