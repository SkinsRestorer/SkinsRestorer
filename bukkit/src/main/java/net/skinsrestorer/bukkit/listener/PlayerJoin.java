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
package net.skinsrestorer.bukkit.listener;

import ch.jalu.configme.SettingsManager;
import lombok.Setter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.SkinsRestorerAPI;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.listeners.SRLoginProfileEvent;
import net.skinsrestorer.shared.listeners.SharedLoginProfileListener;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.SkinStorage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin extends SharedLoginProfileListener<Void> implements Listener {
    @Setter
    private static boolean resourcePack;
    private final ISRPlugin plugin;

    public PlayerJoin(SettingsManager settings, SkinStorage skinStorage, ISRPlugin plugin) {
        super(settings, skinStorage, plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        if (resourcePack && settings.getProperty(Config.RESOURCE_PACK_FIX))
            return;

        handleLogin(wrap(event));
    }

    private SRLoginProfileEvent<Void> wrap(PlayerJoinEvent event) {
        return new SRLoginProfileEvent<Void>() {
            @Override
            public boolean isOnline() {
                return Bukkit.getOnlineMode();
            }

            @Override
            public String getPlayerName() {
                return event.getPlayer().getName();
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void setResultProperty(IProperty property) {
                SkinsRestorerAPI.getApi().applySkin(new PlayerWrapper(event.getPlayer()), property);
            }

            @Override
            public Void runAsync(Runnable runnable) {
                plugin.runAsync(runnable);
                return null;
            }
        };
    }
}