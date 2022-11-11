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
import lombok.Setter;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.listeners.SRLoginProfileEvent;
import net.skinsrestorer.shared.listeners.SharedLoginProfileListener;
import net.skinsrestorer.shared.storage.Config;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
@Getter
public class PlayerJoin extends SharedLoginProfileListener implements Listener {
    @Setter
    private static boolean resourcePack;
    private final SkinsRestorer plugin;

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        SRLoginProfileEvent profileEvent = wrap(event);

        if (handleSync(profileEvent))
            return;

        if (resourcePack && Config.RESOURCE_PACK_FIX)
            return;

        plugin.runAsync(() -> {
            try {
                handleAsync(profileEvent).ifPresent(property ->
                        plugin.getSkinsRestorerAPI().applySkin(new PlayerWrapper(event.getPlayer()), property));
            } catch (SkinRequestException e) {
                plugin.getSrLogger().debug(e);
            }
        });
    }

    private SRLoginProfileEvent wrap(PlayerJoinEvent event) {
        return new SRLoginProfileEvent() {
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