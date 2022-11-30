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

import org.bukkit.plugin.java.JavaPlugin;

public class SkinsRestorerBootstrap extends JavaPlugin {
    @Override
    public void onEnable() {
        Exception startupError = null;
        SkinsRestorerBukkit skinsRestorerBukkit = null;
        try {
            skinsRestorerBukkit = new SkinsRestorerBukkit(getServer(), getDescription().getVersion(), getDataFolder().toPath(), this, false);
            skinsRestorerBukkit.pluginStartup();
        } catch (Exception e) {
            startupError = e;
        } finally {
            if (skinsRestorerBukkit != null && !skinsRestorerBukkit.isUpdaterInitialized()) {
                skinsRestorerBukkit.updateCheck();
            }
        }

        if (startupError != null) {
            getLogger().severe("An unexpected error occurred while starting the plugin.");
            startupError.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
}
