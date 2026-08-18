package dev.vospek.leviathan.util;

public final class LeviathanConstants {

    private LeviathanConstants() {
    }

    public static final boolean DISABLE_VANILLA_PROFILER = Boolean.getBoolean("Leviathan.disable-vanilla-profiler");
    public static final boolean ENABLE_FMA = Boolean.getBoolean("Leviathan.enableFMA");
    public static final boolean ENABLE_IO_URING = Boolean.getBoolean("Leviathan.enable-io-uring");
    public static final boolean ENABLE_BASE64CODER_WARNING = Boolean.getBoolean("Leviathan.enable-base64coder-warning");
    public static final boolean DISABLE_VANILLA_DEBUG_FEATURE = Boolean.getBoolean("Leviathan.disable-vanilla-debug-feature");
    public static final String LINEAR_V2_READ_ONLY_FLAG = "Leviathan.linear-v2-read-only";
    public static final boolean LINEAR_V2_READ_ONLY = Boolean.getBoolean(LINEAR_V2_READ_ONLY_FLAG);

    public static final String DISABLE_VANILLA_PROFILER_DOCS_URL = "https://gitlab.com/vospek/minecraft-dev/leviathan";
}
