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
import lombok.Setter;
import net.skinsrestorer.bukkit.utils.EventWrapper;
import net.skinsrestorer.shared.config.ServerConfig;
import net.skinsrestorer.shared.listeners.LoginProfileListenerAdapter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerJoinListener implements Listener {
    private final LoginProfileListenerAdapter<Void> adapter;
    private final EventWrapper eventWrapper;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
//        @todo fix resource pack issue when ResourcePackStatusEvent is called before PlayerJoinEvent
//        if (resourcePack && settings.getProperty(ServerConfig.RESOURCE_PACK_FIX)) {
//            System.out.println("onjoin-CANCEL");
//            return;
//        }
        adapter.handleLogin(eventWrapper.wrap(event));
    }
}
