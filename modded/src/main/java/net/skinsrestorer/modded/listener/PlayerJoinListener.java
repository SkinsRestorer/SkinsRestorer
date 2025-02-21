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
package net.skinsrestorer.modded.listener;

import dev.architectury.event.events.common.PlayerEvent;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.modded.SkinApplierMod;
import net.skinsrestorer.shared.listeners.LoginProfileListenerAdapter;
import net.skinsrestorer.shared.listeners.event.SRLoginProfileEvent;

import javax.inject.Inject;
import java.util.UUID;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerJoinListener implements PlayerEvent.PlayerJoin {
    private final LoginProfileListenerAdapter<Void> adapter;

    @Override
    public void join(ServerPlayer player) {
        adapter.handleLogin(wrap(player));
    }

    private SRLoginProfileEvent<Void> wrap(ServerPlayer player) {
        return new SRLoginProfileEvent<>() {
            @Override
            public boolean hasOnlineProperties() {
                return !player.getGameProfile().getProperties().get(SkinProperty.TEXTURES_NAME).isEmpty();
            }

            @Override
            public UUID getPlayerUniqueId() {
                return player.getGameProfile().getId();
            }

            @Override
            public String getPlayerName() {
                return player.getGameProfile().getName();
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void setResultProperty(SkinProperty property) {
                SkinApplierMod.setGameProfileTextures(player, property);
            }

            @Override
            public Void runAsync(Runnable runnable) {
                runnable.run();
                return null;
            }
        };
    }
}
