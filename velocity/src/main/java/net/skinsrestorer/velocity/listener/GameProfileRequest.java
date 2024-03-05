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

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.listeners.LoginProfileListenerAdapter;
import net.skinsrestorer.shared.listeners.event.SRLoginProfileEvent;
import net.skinsrestorer.velocity.SkinApplierVelocity;

import javax.inject.Inject;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GameProfileRequest {
    private final SkinApplierVelocity skinApplier;
    private final LoginProfileListenerAdapter<EventTask> adapter;

    @Subscribe
    public EventTask onGameProfileRequest(GameProfileRequestEvent event) {
        return adapter.handleLogin(wrap(event));
    }

    private SRLoginProfileEvent<EventTask> wrap(GameProfileRequestEvent event) {
        return new SRLoginProfileEvent<>() {
            @Override
            public boolean hasOnlineProperties() {
                return event.isOnlineMode();
            }

            @Override
            public UUID getPlayerUniqueId() {
                return event.getGameProfile().getId();
            }

            @Override
            public String getPlayerName() {
                return event.getGameProfile().getName();
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void setResultProperty(SkinProperty property) {
                event.setGameProfile(skinApplier.updateProfileSkin(event.getGameProfile(), property));
            }

            @Override
            public EventTask runAsync(Runnable runnable) {
                return EventTask.async(runnable);
            }
        };
    }
}
