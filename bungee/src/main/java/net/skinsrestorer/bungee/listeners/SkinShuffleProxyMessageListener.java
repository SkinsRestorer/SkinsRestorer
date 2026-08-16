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
package net.skinsrestorer.bungee.listeners;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.skinsrestorer.bungee.wrapper.WrapperBungee;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleChannels;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleSupport;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinShuffleProxyMessageListener implements Listener {
    private final WrapperBungee wrapper;
    private final SkinShuffleSupport skinShuffleSupport;

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!SkinShuffleChannels.SKIN_REFRESH.equals(event.getTag())) {
            return;
        }
        if (!(event.getSender() instanceof ProxiedPlayer player) || !(event.getReceiver() instanceof Server)) {
            return;
        }
        if (!skinShuffleSupport.isEnabled()) {
            return;
        }

        event.setCancelled(true);
        skinShuffleSupport.handleSkinRefresh(wrapper.player(player), event.getData());
    }
}
