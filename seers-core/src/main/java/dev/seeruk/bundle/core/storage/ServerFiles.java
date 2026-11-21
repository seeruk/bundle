package dev.seeruk.bundle.core.storage;

import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Utilities for writing files to the server's folder. Paths are relative to the server root, and
 * should never be able to land outside it.
 */
public final class ServerFiles {
    private ServerFiles() {}

    /**
     * Returns an absolute path inside the server root, or empty. Accepts both relative and absolute
     * paths. Only accepts paths actually under the server root (i.e. not the server root itself).
     */
    public static Optional<Path> resolve(MinecraftServer server, String path) {
        return resolve(server.getServerDirectory(), path);
    }

    /**
     * Replaces a file's contents, creating it (and any intermediate folders if necessary). The file
     * is swapped as a whole (a temporary file is written first, then moved).
     *
     * @throws IOException if the file can't be written, or the path is outside the server's folder
     */
    public static void write(MinecraftServer server, String path, String contents) throws IOException {
        write(server.getServerDirectory(), path, contents);
    }

    static Optional<Path> resolve(Path folder, String path) {
        final var base = folder.toAbsolutePath().normalize();
        try {
            final var resolved = base.resolve(path).normalize();
            return resolved.startsWith(base) && !resolved.equals(base) ? Optional.of(resolved) : Optional.empty();
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    static void write(Path folder, String path, String contents) throws IOException {
        final var file = resolve(folder, path)
            .orElseThrow(() -> new IOException("Not writing " + path + ", as it's outside the server's folder"));

        Files.createDirectories(file.getParent());
        final var temp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temp, contents);
            move(temp, file);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, ATOMIC_MOVE, REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, REPLACE_EXISTING);
        }
    }
}
