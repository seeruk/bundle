package dev.seeruk.bundle.digest;

import dev.seeruk.bundle.core.storage.WorldStorage;
import dev.seeruk.bundle.digest.api.Issue;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.seeruk.bundle.digest.SeersDigest.MOD_ID;

/**
 * Every issue that's been published, kept forever, so any can be read again.
 */
public final class Archive {
    // Oldest first
    private static final WorldStorage<List<Issue>> ISSUES = WorldStorage.create(
        MOD_ID, "issues", Issue.CODEC.listOf(), List::of
    );

    private Archive() {}

    /**
     * Every issue, oldest first.
     */
    public static List<Issue> all(MinecraftServer server) {
        return ISSUES.get(server);
    }

    public static Optional<Issue> latest(MinecraftServer server) {
        final var issues = all(server);
        return issues.isEmpty() ? Optional.empty() : Optional.of(issues.getLast());
    }

    public static Optional<Issue> get(MinecraftServer server, int number) {
        return all(server).stream().filter(issue -> issue.number() == number).findFirst();
    }

    /**
     * The number the next issue should have.
     */
    public static int nextIssueNumber(MinecraftServer server) {
        return latest(server).map(issue -> issue.number() + 1).orElse(1);
    }

    public static void add(MinecraftServer server, Issue issue) {
        ISSUES.update(
            server, issues -> {
                final var updated = new ArrayList<>(issues);
                updated.add(issue);
                return List.copyOf(updated);
            }
        );
    }
}
