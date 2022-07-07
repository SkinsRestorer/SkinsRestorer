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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.listeners.LoginProfileEvent;
import net.skinsrestorer.shared.listeners.LoginProfileListener;
import net.skinsrestorer.shared.storage.Config;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

@Getter
@RequiredArgsConstructor
public class PlayerResourcePackStatus extends LoginProfileListener implements Listener {
    private final SkinsRestorer plugin;
    private final boolean isOnlineMode = Bukkit.getOnlineMode();

    @EventHandler
    public void onResourcePackStatus(final PlayerResourcePackStatusEvent event) {
        if (!Config.RESOURCE_PACK_FIX)
            return;

        PlayerJoin.setResourcePack(true);

        LoginProfileEvent profileEvent = wrap(event);

        if (handleSync(profileEvent))
            return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                handleAsync(profileEvent).ifPresent(name -> {
                    try {
                        plugin.getSkinsRestorerAPI().applySkin(new PlayerWrapper(event.getPlayer()), name);
                    } catch (SkinRequestException e) {
                        plugin.getSrLogger().debug(e);
                    }
                }));
    }

    private LoginProfileEvent wrap(PlayerResourcePackStatusEvent event) {
        return new LoginProfileEvent() {
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
        };
    }
}
