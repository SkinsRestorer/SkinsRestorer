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
package net.skinsrestorer.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.listeners.AdminInfoListenerAdapter;
import net.skinsrestorer.shared.listeners.event.SRServerConnectedEvent;
import net.skinsrestorer.velocity.wrapper.WrapperVelocity;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AdminInfoListener {
    private final WrapperVelocity wrapper;
    private final AdminInfoListenerAdapter adapter;

    @Subscribe(order = PostOrder.LAST)
    public void onServerConnected(ServerConnectedEvent event) {
        adapter.handleConnect(wrap(event));
    }

    private SRServerConnectedEvent wrap(ServerConnectedEvent event) {
        return () -> wrapper.player(event.getPlayer());
    }
}
