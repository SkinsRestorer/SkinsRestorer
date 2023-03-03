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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class SemanticVersion {
    private final int major;
    private final int minor;
    private final int patch;

    public static SemanticVersion fromString(String version) {
        // Sanitize version
        version = version.replace("v", "");
        version = version.replace("-SNAPSHOT", "");

        String[] split = version.split("\\.");
        return new SemanticVersion(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }

    public boolean isNewerThan(SemanticVersion otherVersion) {
        if (otherVersion.major > major) {
            return false;
        } else if (otherVersion.major < major) {
            return true;
        } else if (otherVersion.minor > minor) {
            return false;
        } else if (otherVersion.minor < minor) {
            return true;
        } else if (otherVersion.patch > patch) {
            return false;
        } else return otherVersion.patch < patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
