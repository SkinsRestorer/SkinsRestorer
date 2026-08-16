/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
package net.skinsrestorer.shared.config;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public class ProxyConfig implements SettingsHolder {
    @Comment("Enables command restrictions for backend servers.")
    public static final Property<Boolean> NOT_ALLOWED_COMMAND_SERVERS_ENABLED = newProperty("proxy.notAllowedCommandServers.enabled", true);
    @Comment("Blocks SkinsRestorer commands until the player joins a backend server.")
    public static final Property<Boolean> NOT_ALLOWED_COMMAND_SERVERS_IF_NONE_BLOCK_COMMAND = newProperty("proxy.notAllowedCommandServers.ifNoServerBlockCommand", true);
    @Comment({
            "When false, commands are blocked on servers in the list.",
            "When true, commands are allowed only on servers in the list."
    })
    public static final Property<Boolean> NOT_ALLOWED_COMMAND_SERVERS_ALLOWLIST = newProperty("proxy.notAllowedCommandServers.allowList", false);
    @Comment("The backend servers controlled by proxy.notAllowedCommandServers.allowList.")
    public static final Property<List<String>> NOT_ALLOWED_COMMAND_SERVERS = newListProperty("proxy.notAllowedCommandServers.list", List.of("auth"));

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("proxy",
                "\n",
                "\n#########",
                "\n# Proxy #",
                "\n#########",
                "\n",
                "Controls behavior for proxy networks."
        );
        conf.setComment("proxy.notAllowedCommandServers",
                "Restricts SkinsRestorer commands on selected backend servers.",
                "These options apply only to proxy networks such as BungeeCord and Velocity."
        );
    }
}
