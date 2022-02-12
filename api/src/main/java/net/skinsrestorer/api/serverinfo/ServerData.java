/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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
package net.skinsrestorer.api.serverinfo;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public class ServerData {
    protected static final String[] HYBRIDS = {
            "mohist"
    };
    private final boolean hybrid;
    private final String serverName;

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
            spigot = isClassPresent("org.spigotmc.SpigotConfig");
            paper = isClassPresent("com.destroystokyo.paper.PaperConfig");
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
