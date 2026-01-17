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
package net.skinsrestorer.mod.listener;

import dev.architectury.event.events.common.PlayerEvent;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.skinsrestorer.mod.wrapper.WrapperMod;
import net.skinsrestorer.shared.listeners.AdminInfoListenerAdapter;
import net.skinsrestorer.shared.listeners.event.SRServerConnectedEvent;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AdminInfoListener implements PlayerEvent.PlayerJoin {
    private final WrapperMod wrapper;
    private final AdminInfoListenerAdapter adapter;

    @Override
    public void join(ServerPlayer player) {
        adapter.handleConnect(wrap(player));
    }

    private SRServerConnectedEvent wrap(ServerPlayer player) {
        return () -> wrapper.player(player);
    }
}
