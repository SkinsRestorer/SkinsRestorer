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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.shared.listeners.SRLoginProfileEvent;
import net.skinsrestorer.shared.listeners.SharedLoginProfileListener;
import net.skinsrestorer.sponge.SkinsRestorer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent.Auth;
import org.spongepowered.api.profile.GameProfile;

import java.util.Optional;

@RequiredArgsConstructor
@Getter
public class LoginListener extends SharedLoginProfileListener implements EventListener<ClientConnectionEvent.Auth> {
    private final SkinsRestorer plugin;

    @Override
    public void handle(@NotNull Auth event) {
        SRLoginProfileEvent wrapped = wrap(event);
        if (handleSync(wrapped))
            return;

        final GameProfile profile = event.getProfile();
        profile.getName().flatMap(name -> {
            try {
                return handleAsync(wrapped);
            } catch (SkinRequestException e) {
                plugin.getSrLogger().debug(e);
                return Optional.empty();
            }
        }).ifPresent(property -> plugin.getSkinApplierSponge().updateProfileSkin(profile, property));
    }

    private SRLoginProfileEvent wrap(Auth event) {
        return new SRLoginProfileEvent() {
            @Override
            public boolean isOnline() {
                return Sponge.getServer().getOnlineMode();
            }

            @Override
            public String getPlayerName() {
                return event.getProfile().getName().orElseThrow(() -> new RuntimeException("Could not get player name!"));
            }

            @Override
            public boolean isCancelled() {
                return event.isCancelled();
            }
        };
    }
}
