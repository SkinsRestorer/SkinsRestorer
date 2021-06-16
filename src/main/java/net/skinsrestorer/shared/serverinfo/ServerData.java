package net.skinsrestorer.shared.serverinfo;

import io.papermc.lib.PaperLib;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public class ServerData {
    private final boolean hybrid;
    private final String serverName;
    protected static final String[] HYBRIDS = {
            "mohist"
    };

    public static ServerData determineEnvironment(Platform platform, String version) {
        ClassInfo info = new ClassInfo(platform);
        boolean hybrid2 = (platform == Platform.BUNGEECORD && info.isVelocity())
                || (platform == Platform.VELOCITY && info.isBungeecord())
                || (platform == Platform.SPONGE && info.isCraftBukkit())
                || (platform == Platform.BUKKIT && (info.isSpongeVanilla() || info.isSpongeForge()))
                || Arrays.stream(HYBRIDS).anyMatch(str -> str.equals(version.toLowerCase()));

        return new ServerData(hybrid2, version);
    }

    @Data
    private static class ClassInfo {
        private final boolean craftBukkit;
        private final boolean spigot;
        private final boolean paper;

        private final boolean spongeAPI;
        private final boolean spongeVanilla;
        private final boolean spongeForge;

        private final boolean bungeecord;
        private final boolean velocity;

        public ClassInfo(Platform platform) {
            spigot = PaperLib.isSpigot();
            paper = PaperLib.isSpigot();
            craftBukkit = platform == Platform.BUKKIT || isClassPresent("org.bukkit.Bukkit");
            spongeVanilla = isClassPresent("org.spongepowered.server.SpongeVanilla");
            spongeForge = isClassPresent("org.spongepowered.mod.SpongeCoremod");
            spongeAPI = platform == Platform.SPONGE || isClassPresent("org.spongepowered.api.Sponge");
            bungeecord = platform == Platform.BUNGEECORD || isClassPresent("net.md_5.bungee.BungeeCord");
            velocity = platform == Platform.VELOCITY || isClassPresent("com.velocitypowered.proxy.Velocity");
        }

        private boolean isClassPresent(String... classNames) {
            for (String className : classNames) {
                try {
                    Class.forName(className);
                    return true;
                } catch (ClassNotFoundException ignored) {
                }
            }

            return false;
        }
    }
}
