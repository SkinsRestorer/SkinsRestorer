package net.skinsrestorer.shared.serverinfo;

import lombok.Data;

@Data
public class ClassInfo {
    private final boolean craftBukkit;
    private final boolean spigot;
    private final boolean paper;
    private final boolean folia;

    private final boolean spongeAPI;
    private final boolean spongeVanilla;
    private final boolean spongeForge;

    private final boolean bungeecord;
    private final boolean velocity;

    private static final ClassInfo INSTANCE = new ClassInfo();

    public static ClassInfo get() {
        return INSTANCE;
    }

    private ClassInfo() {
        spigot = isClassPresent("org.spigotmc.SpigotConfig");
        paper = isClassPresent("com.destroystokyo.paper.PaperConfig");
        craftBukkit = isClassPresent("org.bukkit.Bukkit");
        folia = isClassPresent("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
        spongeVanilla = isClassPresent("org.spongepowered.server.SpongeVanilla");
        spongeForge = isClassPresent("org.spongepowered.mod.SpongeCoremod");
        spongeAPI = isClassPresent("org.spongepowered.api.Sponge");
        bungeecord = isClassPresent("net.md_5.bungee.BungeeCord");
        velocity = isClassPresent("com.velocitypowered.proxy.Velocity");
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
