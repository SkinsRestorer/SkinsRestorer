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
package net.skinsrestorer.velocity.listener;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleChannels;
import net.skinsrestorer.shared.integration.skinshuffle.SkinShuffleSupport;
import net.skinsrestorer.velocity.wrapper.WrapperVelocity;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SkinShuffleProxyMessageListener implements EventHandler<PluginMessageEvent> {
    private final WrapperVelocity wrapper;
    private final SkinShuffleSupport skinShuffleSupport;

    @Override
    public void execute(PluginMessageEvent event) {
        if (!SkinShuffleChannels.SKIN_REFRESH.equals(event.getIdentifier().getId())) {
            return;
        }
        if (!(event.getSource() instanceof Player player) || !(event.getTarget() instanceof ServerConnection)) {
            return;
        }
        if (!skinShuffleSupport.isEnabled()) {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());
        skinShuffleSupport.handleSkinRefresh(wrapper.player(player), event.getData());
    }
}
