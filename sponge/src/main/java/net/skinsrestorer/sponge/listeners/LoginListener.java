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

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.listeners.LoginProfileListenerAdapter;
import net.skinsrestorer.shared.listeners.event.SRLoginProfileEvent;
import net.skinsrestorer.shared.reflection.ReflectionUtil;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LoginListener implements EventListener<ServerSideConnectionEvent.Auth> {
    private final LoginProfileListenerAdapter<Void> adapter;
    private final SRLogger logger;

    @Override
    public void handle(@NotNull ServerSideConnectionEvent.Auth event) {
        try {
            adapter.handleLogin(wrap(event));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private SRLoginProfileEvent<Void> wrap(ServerSideConnectionEvent.Auth event) {
        logger.info(String.valueOf(event));

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
                return false;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void setResultProperty(SkinProperty property) {
                try {
                    GameProfile gameProfile = (GameProfile) ReflectionUtil.getFieldByType(event.connection(), "GameProfile");
                    gameProfile.getProperties().removeAll(SkinProperty.TEXTURES_NAME);
                    gameProfile.getProperties().put(SkinProperty.TEXTURES_NAME, new Property(SkinProperty.TEXTURES_NAME, property.getValue(), property.getSignature()));
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Void runAsync(Runnable runnable) {
                runnable.run();
                return null;
            }
        };
    }
}
