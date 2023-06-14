/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.skinsrestorer.shared.serverinfo;

import lombok.Data;

@Data
public class ClassInfo {
    private static final ClassInfo INSTANCE = new ClassInfo();
    private final boolean craftBukkit;
    private final boolean spigot;
    private final boolean paper;
    private final boolean folia;
    private final boolean spongeAPI;
    private final boolean spongeVanilla;
    private final boolean spongeForge;
    private final boolean bungeecord;
    private final boolean velocity;

    private ClassInfo() {
        spigot = isClassPresent("org.spigotmc.SpigotConfig");
        paper = isClassPresent("com.destroystokyo.paper.PaperConfig", "io.papermc.paper.configuration.Configuration");
        craftBukkit = isClassPresent("org.bukkit.Bukkit");
        folia = isClassPresent("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
        spongeVanilla = isClassPresent("org.spongepowered.server.SpongeVanilla");
        spongeForge = isClassPresent("org.spongepowered.mod.SpongeCoremod");
        spongeAPI = isClassPresent("org.spongepowered.api.Sponge");
        bungeecord = isClassPresent("net.md_5.bungee.BungeeCord");
        velocity = isClassPresent("com.velocitypowered.proxy.Velocity");
    }

    public static ClassInfo get() {
        return INSTANCE;
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
