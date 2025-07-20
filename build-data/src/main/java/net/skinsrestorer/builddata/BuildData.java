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

import java.io.IOException;
import java.util.Properties;

@ApiStatus.Internal
public class BuildData {

    public static final String VERSION;
    public static final String DESCRIPTION;
    public static final String URL;
    public static final String COMMIT;
    public static final String BRANCH;
    public static final String BUILD_TIME;
    public static final String CI_NAME;
    public static final String CI_BUILD_NUMBER;
    public static final String COMMIT_SHORT;
    public static final String[] LOCALES;

    static {
        Properties properties = new Properties();
        try (var inputStream = BuildData.class.getClassLoader().getResourceAsStream("sr-build-data.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException("Build data properties file not found");
            }

            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load build data properties", e);
        }

        VERSION = properties.getProperty("version");
        DESCRIPTION = properties.getProperty("description");
        URL = properties.getProperty("url");
        COMMIT = properties.getProperty("commit");
        BRANCH = properties.getProperty("branch");
        BUILD_TIME = properties.getProperty("build_time");
        CI_NAME = properties.getProperty("ci_name");
        CI_BUILD_NUMBER = properties.getProperty("ci_build_number");
        COMMIT_SHORT = COMMIT.substring(0, 7);
        LOCALES = properties.getProperty("locales").split("\\|");
    }

    private BuildData() {
    }
}
