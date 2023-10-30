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
package net.skinsrestorer.shared.connections.requests;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import net.skinsrestorer.shared.serverinfo.ServerInfo;

@AllArgsConstructor
@SuppressWarnings("unused")
@SuppressFBWarnings({"UWF_UNWRITTEN_FIELD", "URF_UNREAD_FIELD"})
public class DumpInfo {
    private String pluginVersion;
    private Boolean proxyMode;
    private String platformVersion;
    private ServerInfo serverInfo;
    private DumpInfo.PlatformType platformType;
    private OSInfo osInfo;
    private JavaInfo javaInfo;
    private UserInfo userInfo;

    public enum PlatformType {
        SERVER,
        PROXY
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
