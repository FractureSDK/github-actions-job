package dev.vospek.leviathan.async.world;

import dev.vospek.leviathan.config.LeviathanConfig;

import java.util.Locale;

public enum UnsafeReadPolicy {
    STRICT,
    BUFFERED,
    DISABLED;

    public static UnsafeReadPolicy fromString(String readPolicy) {
        try {
            return valueOf(readPolicy.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LeviathanConfig.LOGGER.warn("Invalid unsafe read policy: {}, falling back to {}.", readPolicy, DISABLED.toString());
            return DISABLED;
        }
    }
}
