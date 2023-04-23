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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class SemanticVersion {
    private final int[] version;

    public SemanticVersion(int major, int minor, int patch) {
        this(new int[]{major, minor, patch});
    }

    public static SemanticVersion fromString(String version) {
        // Sanitize version
        version = version.replace("v", "");
        version = version.replace("-SNAPSHOT", "");

        String[] split = version.split("\\.");
        return new SemanticVersion(Arrays.stream(split).mapToInt(Integer::parseInt).toArray());
    }

    public boolean isNewerThan(SemanticVersion otherVersion) {
        int i = 0;
        for (int version : version) {
            if (i == otherVersion.version.length) {
                return true;
            }

            int otherVersionPart = otherVersion.version[i];
            if (version > otherVersionPart) {
                return true;
            } else if (version < otherVersionPart) {
                return false;
            }

            i++;
        }

        return false;
    }

    @Override
    public String toString() {
        return String.join(".", Arrays.stream(version).mapToObj(String::valueOf).toArray(String[]::new));
    }
}
