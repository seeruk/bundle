package dev.seeruk.bundle.digest.command;

import dev.seeruk.bundle.digest.api.Issue;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DigestCommandTest {
    private static final UUID SEER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ALEX = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void describesIssuesWithTheirDatesAsTheBookWritesThem() {
        final var issue = new Issue(
            3,
            Instant.parse("2026-11-16T18:00:00Z"),
            Instant.parse("2026-11-23T18:00:00Z"),
            Map.of(),
            List.of(SEER, ALEX),
            List.of(ALEX),
            Issue.Moments.EMPTY
        );

        assertEquals("<gold>#3</gold>: 16 to 23 Nov 2026, 2 players (1 new)", DigestCommand.describe(issue, ZoneOffset.UTC));
    }
}
