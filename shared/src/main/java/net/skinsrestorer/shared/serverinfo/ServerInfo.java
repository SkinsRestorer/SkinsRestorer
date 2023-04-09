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
package net.skinsrestorer.shared.serverinfo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ServerInfo {
    private final boolean hybrid;
    private final Platform platform;
    private final ClassInfo classInfo;

    public static ServerInfo determineEnvironment(Platform platform) {
        ClassInfo info = ClassInfo.get();
        boolean hybrid2 = (platform == Platform.BUNGEE_CORD && info.isVelocity())
                || (platform == Platform.VELOCITY && info.isBungeecord())
                || (platform == Platform.SPONGE && info.isCraftBukkit())
                || (platform == Platform.BUKKIT && (info.isSpongeVanilla() || info.isSpongeForge()));

        return new ServerInfo(hybrid2, platform, info);
    }
}
