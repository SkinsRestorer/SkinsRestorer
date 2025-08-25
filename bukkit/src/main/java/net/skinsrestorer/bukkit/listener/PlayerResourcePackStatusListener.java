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
package net.skinsrestorer.bukkit.listener;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.utils.EventWrapper;
import net.skinsrestorer.shared.config.ServerConfig;
import net.skinsrestorer.shared.listeners.LoginProfileListenerAdapter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerResourcePackStatusListener implements Listener {
    private final SettingsManager settings;
    private final LoginProfileListenerAdapter<Void> adapter;
    private final EventWrapper eventWrapper;

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (event.getStatus().equals(PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED)
                && !settings.getProperty(ServerConfig.RESOURCE_PACK_FIX)) {
            adapter.handleLogin(eventWrapper.wrap(event));
        }
    }
}
