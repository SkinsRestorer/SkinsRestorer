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
package net.skinsrestorer.sponge.listeners;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.listeners.LoginProfileListenerAdapter;
import net.skinsrestorer.shared.listeners.event.SRLoginProfileEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent.Login;
import org.spongepowered.api.profile.property.ProfileProperty;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LoginListener implements EventListener<Login> {
    private final LoginProfileListenerAdapter<Void> adapter;

    @Override
    public void handle(@NotNull ServerSideConnectionEvent.Login event) {
        adapter.handleLogin(wrap(event));
    }

    private SRLoginProfileEvent<Void> wrap(Login event) {
        return new SRLoginProfileEvent<Void>() {
            @Override
            public boolean isOnline() {
                return event.profile().properties().stream().anyMatch(p -> p.name().equals(SkinProperty.TEXTURES_NAME));
            }

            @Override
            public String getPlayerName() {
                return event.profile().name().orElseThrow(() -> new RuntimeException("Could not get player name!"));
            }

            @Override
            public boolean isCancelled() {
                return event.isCancelled();
            }

            @Override
            public void setResultProperty(SkinProperty property) {
                event.user().offer(Keys.UPDATE_GAME_PROFILE, true);
                event.user().offer(Keys.SKIN_PROFILE_PROPERTY,
                        ProfileProperty.of(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));
            }

            @Override
            public Void runAsync(Runnable runnable) {
                return null;
            }
        };
    }
}
