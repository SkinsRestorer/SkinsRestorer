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
package net.skinsrestorer.shared.config;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.configurationdata.CommentsConfiguration;
import ch.jalu.configme.properties.Property;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static net.skinsrestorer.shared.utils.FluentList.listOf;

public class ProxyConfig implements SettingsHolder {
    public static final Property<Boolean> NOT_ALLOWED_COMMAND_SERVERS_ENABLED = newProperty("proxy.notAllowedCommandServers.enabled", true);
    @Comment("Whether ONLY servers from the list below are allowed to use SkinsRestorer commands.")
    public static final Property<Boolean> NOT_ALLOWED_COMMAND_SERVERS_ALLOWLIST = newProperty("proxy.notAllowedCommandServers.allowList", false);
    @Comment("Block players from executing SkinsRestorer commands before having joined a server. (RECOMMENDED)")
    public static final Property<Boolean> NOT_ALLOWED_COMMAND_SERVERS_IF_NONE_BLOCK_COMMAND = newProperty("proxy.notAllowedCommandServers.ifNoServerBlockCommand", false);
    public static final Property<List<String>> NOT_ALLOWED_COMMAND_SERVERS = newListProperty("proxy.notAllowedCommandServers.List", listOf("auth"));

    @Override
    public void registerComments(CommentsConfiguration conf) {
        conf.setComment("proxy.notAllowedCommandServers",
                "Disable all SkinsRestorer commands on specific backend servers.",
                "[!] This only works & is relevant if you're using proxies like bungee / waterfall"
        );
    }
}
