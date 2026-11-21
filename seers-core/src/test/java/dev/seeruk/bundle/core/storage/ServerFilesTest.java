package dev.seeruk.bundle.core.storage;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFilesTest {
    @TempDir
    private Path folder;

    @Nested
    class Resolve {
        static Stream<Arguments> shouldResolveInsideTheFolder() {
            return Stream.of(
                Arguments.of("Resolves a file", "export.csv", "export.csv"),
                Arguments.of("Resolves a file in a folder", "exports/zones.csv", "exports/zones.csv"),
                Arguments.of("Allows going up and back in", "exports/../zones.csv", "zones.csv")
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldResolveInsideTheFolder(final String description, final String path, final String expected) {
            assertEquals(Optional.of(folder.resolve(expected)), ServerFiles.resolve(folder, path));
        }

        static Stream<Arguments> shouldRefuseOutsideTheFolder() {
            return Stream.of(
                Arguments.of("Refuses going up out of it", "../export.csv"),
                Arguments.of("Refuses going up out of it from a folder", "exports/../../export.csv"),
                Arguments.of("Refuses an absolute path elsewhere", "/tmp/export.csv"),
                Arguments.of("Refuses the folder itself", "."),
                Arguments.of("Refuses an empty path", "")
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldRefuseOutsideTheFolder(final String description, final String path) {
            assertEquals(Optional.empty(), ServerFiles.resolve(folder, path));
        }

        @Test
        void allowsAnAbsolutePathInsideTheFolder() {
            final var path = folder.resolve("export.csv");

            assertEquals(Optional.of(path), ServerFiles.resolve(folder, path.toString()));
        }
    }

    @Nested
    class Write {
        @Test
        void writesTheFileAndItsFolders() throws IOException {
            ServerFiles.write(folder, "exports/zones.csv", "Testing");

            assertEquals("Testing", Files.readString(folder.resolve("exports/zones.csv")));
        }

        // Paths are given the test's folder, which the server's folder is inside, so anything
        // written outside the server's folder can still be found
        static Stream<Arguments> shouldNotWriteOutsideTheFolder() {
            return Stream.of(
                Arguments.of("Going up out of it", (Function<Path, String>) outside -> "../zones.csv"),
                Arguments.of(
                    "Going up out of it from a folder",
                    (Function<Path, String>) outside -> "exports/../../zones.csv"
                ),
                Arguments.of(
                    "An absolute path elsewhere",
                    (Function<Path, String>) outside -> outside.resolve("zones.csv").toString()
                )
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource
        void shouldNotWriteOutsideTheFolder(final String description, final Function<Path, String> path)
            throws IOException {
            final var server = folder.resolve("server");

            assertThrows(IOException.class, () -> ServerFiles.write(server, path.apply(folder), "contents"));
            try (var files = Files.list(folder)) {
                assertEquals(List.of(), files.toList());
            }
        }
    }
}
