package dev.vospek.leviathan.version;

import org.galemc.gale.version.AbstractPaperVersionFetcher;

public class LeviathanVersionFetcher extends AbstractPaperVersionFetcher {

    public static final String DOWNLOAD_PAGE = "https://gitlab.com/vospek/minecraft-dev/leviathan/-/releases";
    public static final String USER_AGENT = null;

    public LeviathanVersionFetcher() {
        super(
            DOWNLOAD_PAGE,
            "Vospek",
            "Leviathan",
            "vospek",
            "minecraft-dev/leviathan",
            ApiType.GITHUB
        );
    }
}
