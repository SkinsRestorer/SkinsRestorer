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
package net.skinsrestorer.shared.connections.requests;

import com.google.gson.JsonObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.builddata.BuildData;
import net.skinsrestorer.shared.info.EnvironmentInfo;
import net.skinsrestorer.shared.info.PlatformInfo;

@RequiredArgsConstructor
@SuppressWarnings("unused")
@SuppressFBWarnings({"UWF_UNWRITTEN_FIELD", "URF_UNREAD_FIELD"})
public class DumpInfo {
    private final BuildInfo buildInfo;
    private final PluginInfo pluginInfo;
    private final EnvironmentInfo environmentInfo;
    private final PlatformInfo platformInfo;
    private final OSInfo osInfo;
    private final JavaInfo javaInfo;
    private final UserInfo userInfo;

    @SuppressWarnings("unused")
    @SuppressFBWarnings({"UWF_UNWRITTEN_FIELD", "URF_UNREAD_FIELD"})
    public static class BuildInfo {
        private final String version = BuildData.VERSION;
        private final String commit = BuildData.COMMIT;
        private final String branch = BuildData.BRANCH;
        private final String buildTime = BuildData.BUILD_TIME;
        private final String ciName = BuildData.CI_NAME;
        private final String ciBuildNumber = BuildData.CI_BUILD_NUMBER;
    }

    @RequiredArgsConstructor
    @SuppressWarnings("unused")
    @SuppressFBWarnings({"UWF_UNWRITTEN_FIELD", "URF_UNREAD_FIELD"})
    public static class PluginInfo {
        private final Boolean proxyMode;
        private final StorageType storageType;
        private final JsonObject configData;

        public enum StorageType {
            NONE,
            FILE,
            MYSQL,
            POSTGRESQL
        }
    }

    // Helps to figure out OS-specific issues
    @SuppressWarnings("unused")
    @SuppressFBWarnings({"UWF_UNWRITTEN_FIELD", "URF_UNREAD_FIELD"})
    public static class OSInfo {
        private final String name = System.getProperty("os.name");
        private final String version = System.getProperty("os.version");
        private final String arch = System.getProperty("os.arch");
    }

    // Helps to figure out java version related issues
    @SuppressWarnings("unused")
    @SuppressFBWarnings({"UWF_UNWRITTEN_FIELD", "URF_UNREAD_FIELD"})
    public static class JavaInfo {
        private final String version = System.getProperty("java.version");
        private final String vendor = System.getProperty("java.vendor");
        private final String vmVersion = System.getProperty("java.vm.version");
        private final String vmVendor = System.getProperty("java.vm.vendor");
        private final String vmName = System.getProperty("java.vm.name");
    }

    // Helps to figure out whether the server is running on a VPS/container or not
    @SuppressWarnings("unused")
    @SuppressFBWarnings({"UWF_UNWRITTEN_FIELD", "URF_UNREAD_FIELD"})
    public static class UserInfo {
        private final String name = System.getProperty("user.name");
        private final String home = System.getProperty("user.home");
        private final String dir = System.getProperty("user.dir");
    }
}
