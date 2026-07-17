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
package net.skinsrestorer.fand;

import io.fand.api.event.player.AsyncPlayerPreLoginEvent;
import io.fand.api.player.PlayerSkin;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.listeners.LoginProfileListenerAdapter;
import net.skinsrestorer.shared.listeners.event.SRLoginProfileEvent;

import javax.inject.Inject;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class FandLoginProfileListener {
    private final LoginProfileListenerAdapter<Void> adapter;

    public void login(AsyncPlayerPreLoginEvent event) {
        adapter.handleLogin(new SRLoginProfileEvent<>() {
            @Override
            public boolean hasOnlineProperties() {
                return event.profile().skin().isPresent();
            }

            @Override
            public UUID getPlayerUniqueId() {
                return event.uniqueId();
            }

            @Override
            public String getPlayerName() {
                return event.name();
            }

            @Override
            public boolean isCancelled() {
                return event.result() != AsyncPlayerPreLoginEvent.Result.ALLOWED;
            }

            @Override
            public void setResultProperty(SkinProperty property) {
                event.setProfile(event.profile().withSkin(
                        new PlayerSkin(property.getValue(), property.getSignature())));
            }

            @Override
            public Void runAsync(Runnable runnable) {
                runnable.run();
                return null;
            }
        });
    }
}
