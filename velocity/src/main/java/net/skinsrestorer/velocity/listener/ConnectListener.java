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
package net.skinsrestorer.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.listeners.ConnectListenerAdapter;
import net.skinsrestorer.shared.listeners.SRServerConnectedEvent;
import net.skinsrestorer.velocity.utils.WrapperVelocity;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConnectListener {
    private final WrapperVelocity wrapper;
    private final ConnectListenerAdapter adapter;

    @Subscribe(order = PostOrder.LAST)
    public void onServerConnect(ServerConnectedEvent event) {
        adapter.handleConnect(wrap(event));
    }

    private SRServerConnectedEvent wrap(ServerConnectedEvent event) {
        return () -> wrapper.player(event.getPlayer());
    }
}
