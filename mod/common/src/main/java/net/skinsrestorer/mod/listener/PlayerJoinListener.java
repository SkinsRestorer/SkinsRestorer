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

import com.mojang.authlib.GameProfile;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.mod.SkinApplierMod;
import net.skinsrestorer.shared.listeners.LoginProfileListenerAdapter;
import net.skinsrestorer.shared.listeners.event.SRLoginProfileEvent;

import javax.inject.Inject;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerJoinListener {
    public static PlayerJoinListener INSTANCE;
    private final LoginProfileListenerAdapter<Void> adapter;

    {
        INSTANCE = this;
    }

    public void join(GameProfile gameProfile) {
        adapter.handleLogin(wrap(gameProfile));
    }

    private SRLoginProfileEvent<Void> wrap(GameProfile gameProfile) {
        return new SRLoginProfileEvent<>() {
            @Override
            public boolean hasOnlineProperties() {
                return !gameProfile.properties().get(SkinProperty.TEXTURES_NAME).isEmpty();
            }

            @Override
            public UUID getPlayerUniqueId() {
                return gameProfile.id();
            }

            @Override
            public String getPlayerName() {
                return gameProfile.name();
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void setResultProperty(SkinProperty property) {
                SkinApplierMod.setGameProfileTextures(gameProfile, property);
            }

            @Override
            public Void runAsync(Runnable runnable) {
                runnable.run();
                return null;
            }
        };
    }
}
