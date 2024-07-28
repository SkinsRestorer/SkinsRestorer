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
package net.skinsrestorer.api.semver;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;

@Getter
@EqualsAndHashCode
@ApiStatus.Internal
public class SemanticVersion {
    // Requires at least three parts
    // Filled with zeros at the end if needed
    private final int[] version;

    public SemanticVersion(int[] version) {
        this.version = fillZerosIfNeeded(version);
    }

    private static int[] fillZerosIfNeeded(int[] version) {
        int[] newVersion = new int[Math.max(3, version.length)];
        System.arraycopy(version, 0, newVersion, 0, version.length);
        return newVersion;
    }

    public SemanticVersion(int major, int minor, int patch) {
        this(new int[]{major, minor, patch});
    }

    public static SemanticVersion fromString(String version) {
        int firstDigit = 0;
        for (int i = 0; i < version.length(); i++) {
            if (Character.isDigit(version.charAt(i))) {
                firstDigit = i;
                break;
            }
        }

        int lastDigit = version.length();
        for (int i = version.length() - 1; i >= 0; i--) {
            if (Character.isDigit(version.charAt(i))) {
                lastDigit = i;
                break;
            }
        }

        version = version.substring(firstDigit, lastDigit + 1);

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

    public boolean isOlderThan(SemanticVersion otherVersion) {
        return !isNewerThan(otherVersion) && !equals(otherVersion);
    }

    @Override
    public String toString() {
        return String.join(".", Arrays.stream(version)
                .mapToObj(String::valueOf)
                .toArray(String[]::new));
    }
}
