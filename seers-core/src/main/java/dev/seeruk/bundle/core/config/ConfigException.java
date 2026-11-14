package dev.seeruk.bundle.core.config;

/**
 * Thrown if there's an issue with a mod's config, for example, it's not valid.
 * Likely to be thrown during server startup.
 */
public class ConfigException extends RuntimeException {
    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
