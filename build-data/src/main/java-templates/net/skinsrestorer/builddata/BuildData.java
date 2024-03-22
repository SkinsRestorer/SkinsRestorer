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
package net.skinsrestorer.builddata;

import org.jetbrains.annotations.ApiStatus;

// The constants are replaced before compilation
@ApiStatus.Internal
public class BuildData {

    public static final String VERSION = "{{ version }}";
    public static final String DESCRIPTION = "{{ description }}";
    public static final String URL = "{{ url }}";
    public static final String COMMIT = "{{ commit }}";
    public static final String BRANCH = "{{ branch }}";
    public static final String BUILD_TIME = "{{ build_time }}";
    public static final String CI_NAME = "{{ ci_name }}";
    public static final String CI_BUILD_NUMBER = "{{ ci_build_number }}";
    public static final String COMMIT_SHORT = COMMIT.substring(0, 7);
    public static final String[] LOCALES = supportedLocales();

    private static String[] supportedLocales() {
        String values = "{{ locales }}";
        return values.split("\\|");
    }

    private BuildData() {
    }
}
