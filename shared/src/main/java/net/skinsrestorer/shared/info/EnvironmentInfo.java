/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.info;

import net.skinsrestorer.shared.plugin.SRPlatformAdapter;

public record EnvironmentInfo(boolean hybrid, Platform platform, PlatformType platformType, ClassInfo classInfo) {
    public static EnvironmentInfo determineEnvironment(SRPlatformAdapter adapter) {
        ClassInfo info = ClassInfo.get();

        Platform platform = adapter.getPlatform();

        // Find common hybrid class mixes
        boolean hybrid = (platform == Platform.BUNGEE_CORD && info.isVelocity())
                || (platform == Platform.VELOCITY && info.isBungeecord());

        return new EnvironmentInfo(hybrid, platform, platform.getPlatformType(), info);
    }
}
