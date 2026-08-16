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
package net.skinsrestorer.bukkit.listener;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.bukkit.SRBukkitAdapter;
import net.skinsrestorer.bukkit.utils.SchedulerProvider;
import net.skinsrestorer.shared.config.ServerConfig;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleChannels;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleWire;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinShuffleJoinListener implements Listener {
    private static final long HANDSHAKE_DELAY_TICKS = 20L;
    private final SRBukkitAdapter adapter;
    private final SchedulerProvider scheduler;
    private final SettingsManager settings;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!settings.getProperty(ServerConfig.SKINSHUFFLE_SUPPORT)) {
            return;
        }

        scheduler.runSyncToEntityDelayed(event.getPlayer(), () -> {
            if (!event.getPlayer().isOnline()) {
                return;
            }

            event.getPlayer().sendPluginMessage(adapter.getPluginInstance(),
                    SkinShuffleChannels.HANDSHAKE,
                    SkinShuffleWire.createHandshakePayload());
        }, HANDSHAKE_DELAY_TICKS);
    }
}
