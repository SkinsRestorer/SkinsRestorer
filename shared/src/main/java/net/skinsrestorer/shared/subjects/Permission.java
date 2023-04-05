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
package net.skinsrestorer.shared.subjects;

import ch.jalu.configme.SettingsManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.config.CommandConfig;

import java.util.function.Predicate;

@RequiredArgsConstructor(staticName = "of", access = AccessLevel.PROTECTED)
public class Permission {
    @Getter
    private final String permissionString;
    private final boolean defaultPermission;

    public boolean checkPermission(SettingsManager settingsManager, Predicate<String> predicate) {
        if (defaultPermission && settingsManager.getProperty(CommandConfig.SKIN_WITHOUT_PERM)) {
            return true; // Default permissions are true for everyone if the config option is enabled
        }

        System.out.println("Checking permission: " + permissionString);
        boolean result = predicate.test(permissionString);
        System.out.println("Permission result: " + result);
        return result;
    }
}
