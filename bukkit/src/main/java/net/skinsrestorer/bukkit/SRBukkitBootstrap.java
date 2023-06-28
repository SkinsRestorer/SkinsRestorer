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
package net.skinsrestorer.bukkit;

import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.skinsrestorer.bukkit.logger.BukkitConsoleImpl;
import net.skinsrestorer.bukkit.update.BukkitUpdateCheckInit;
import net.skinsrestorer.bukkit.utils.PluginJarProvider;
import net.skinsrestorer.shared.log.JavaLoggerImpl;
import net.skinsrestorer.shared.plugin.SRBootstrapper;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.serverinfo.Platform;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

@SuppressWarnings("unused")
public class SRBukkitBootstrap extends JavaPlugin {
    @Getter
    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        this.adventure = BukkitAudiences.create(this);

        Server server = getServer();
        Path pluginFile = getFile().toPath();
        SRBootstrapper.startPlugin(
                injector -> {
                    injector.register(JavaPlugin.class, this);
                    injector.register(Server.class, server);
                    injector.register(BukkitAudiences.class, this.adventure);
                    injector.register(PluginJarProvider.class, new PluginJarProvider(pluginFile));
                },
                new JavaLoggerImpl(new BukkitConsoleImpl(server.getConsoleSender()), server.getLogger()),
                true,
                SRBukkitAdapter.class,
                BukkitUpdateCheckInit.class,
                SRServerPlugin.class,
                getDescription().getVersion(),
                getDataFolder().toPath(),
                Platform.BUKKIT,
                SRBukkitInit.class);
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }
}
