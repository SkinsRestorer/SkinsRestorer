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
package net.skinsrestorer.bukkit;

import net.skinsrestorer.bukkit.update.BukkitUpdateCheckInit;
import net.skinsrestorer.bukkit.utils.BukkitConsoleImpl;
import net.skinsrestorer.shared.log.JavaLoggerImpl;
import net.skinsrestorer.shared.plugin.SRBootstrapper;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.serverinfo.Platform;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

@SuppressWarnings("unused")
public class SRBukkitBootstrap extends JavaPlugin {
    @Override
    public void onEnable() {
        Server server = getServer();
        Path pluginFile = getFile().toPath();
        SRBootstrapper.startPlugin(
                new JavaLoggerImpl(new BukkitConsoleImpl(server.getConsoleSender()), server.getLogger()),
                true,
                i -> new SRBukkitAdapter(i, server, pluginFile, this),
                BukkitUpdateCheckInit.class,
                SRServerPlugin.class,
                getDescription().getVersion(),
                getDataFolder().toPath(),
                Platform.BUKKIT,
                SRBukkitInit.class);
    }
}
