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
import net.skinsrestorer.shared.utils.ReflectionUtil;

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
        spigot = ReflectionUtil.classExists("org.spigotmc.SpigotConfig");
        paper = ReflectionUtil.classExists("com.destroystokyo.paper.PaperConfig", "io.papermc.paper.configuration.Configuration");
        craftBukkit = ReflectionUtil.classExists("org.bukkit.Bukkit");
        folia = ReflectionUtil.classExists("io.papermc.paper.threadedregions.RegionizedServerInitEvent");
        spongeVanilla = ReflectionUtil.classExists("org.spongepowered.server.SpongeVanilla");
        spongeForge = ReflectionUtil.classExists("org.spongepowered.mod.SpongeCoremod");
        spongeAPI = ReflectionUtil.classExists("org.spongepowered.api.Sponge");
        bungeecord = ReflectionUtil.classExists("net.md_5.bungee.BungeeCord");
        velocity = ReflectionUtil.classExists("com.velocitypowered.proxy.Velocity");
    }

    public static ClassInfo get() {
        return INSTANCE;
    }
}
