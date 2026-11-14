package dev.seeruk.bundle.afk.api;

/**
 * Why a player's AFK status changed.
 */
public enum AfkReason {
    /**
     * They went AFK by not doing anything for long enough.
     */
    TIMEOUT,

    /**
     * They came back by doing something.
     */
    ACTIVITY,

    /**
     * They used {@code /afk}.
     */
    COMMAND
}
