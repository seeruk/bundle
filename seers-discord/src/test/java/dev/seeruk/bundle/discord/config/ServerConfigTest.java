package dev.seeruk.bundle.discord.config;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerConfigTest {
    private static final Path SERVER_DIRECTORY = Path.of("/srv/minecraft");
    private static final Path SERVER_ICON = SERVER_DIRECTORY.resolve("server-icon.png");

    static Stream<Arguments> shouldRefuseNamesDiscordRefuses() {
        return Stream.of(
            Arguments.of("Refuses a blank name", " "),
            Arguments.of("Refuses a name that's too long", "a".repeat(81)),
            Arguments.of("Refuses a name with Discord in it", "Minecraft Discord"),
            Arguments.of("Refuses a name with Discord in it in any case", "DISCORD chat"),
            Arguments.of("Refuses a name with Clyde in it", "Clyde")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldRefuseNamesDiscordRefuses(final String description, final String name) {
        assertTrue(parse(name, "").isError());
    }

    @Test
    void shouldReadNamesDiscordAllows() {
        assertEquals("a".repeat(80), parse("a".repeat(80), "").getOrThrow().name());
    }

    @Test
    void shouldRefuseAvatarUrlsThatAreNotUrls() {
        assertTrue(parse("Minecraft Server", "https://example.com/not a url.png").isError());
    }

    static Stream<Arguments> shouldFindTheAvatar() {
        return Stream.of(
            Arguments.of(
                "Uses a URL",
                "https://cdn.example.com/avatar.png",
                Optional.of(SERVER_ICON),
                Optional.of(URI.create("https://cdn.example.com/avatar.png"))
            ),
            Arguments.of(
                "Uses a file, relative to the server's folder",
                "config/avatar.png",
                Optional.of(SERVER_ICON),
                Optional.of(SERVER_DIRECTORY.resolve("config/avatar.png").toUri())
            ),
            Arguments.of(
                "Uses the server's icon if it's blank",
                "",
                Optional.of(SERVER_ICON),
                Optional.of(SERVER_ICON.toUri())
            ),
            Arguments.of("Has none if it's blank and there's no server icon", "", Optional.empty(), Optional.empty())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void shouldFindTheAvatar(
        final String description,
        final String avatar,
        final Optional<Path> serverIcon,
        final Optional<URI> expected
    ) {
        assertEquals(
            expected,
            new ServerConfig("Minecraft Server", avatar).avatarLocation(SERVER_DIRECTORY, serverIcon)
        );
    }

    private static DataResult<ServerConfig> parse(String name, String avatar) {
        return ServerConfig.CODEC.parse(JavaOps.INSTANCE, Map.of("name", name, "avatar", avatar));
    }
}
