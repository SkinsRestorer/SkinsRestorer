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

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.shared.listeners.LoginProfileEvent;
import net.skinsrestorer.shared.listeners.LoginProfileListener;
import net.skinsrestorer.velocity.SkinsRestorer;

@RequiredArgsConstructor
@Getter
public class GameProfileRequest extends LoginProfileListener {
    private final SkinsRestorer plugin;

    // TODO: make async, add #getSkinForPlayer()
    @Subscribe
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        LoginProfileEvent wrapped = wrap(event);
        if (handleSync(wrapped))
            return;

        handleAsync(wrapped).ifPresent(name -> {
            try {
                event.setGameProfile(plugin.getSkinApplierVelocity().updateProfileSkin(event.getGameProfile(), name));
            } catch (SkinRequestException e) {
                plugin.getSrLogger().debug(e);
            }
        });
    }

    private LoginProfileEvent wrap(GameProfileRequestEvent event) {
        return new LoginProfileEvent() {
            @Override
            public boolean isOnline() {
                return event.isOnlineMode();
            }

            @Override
            public String getPlayerName() {
                return event.getUsername();
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };
    }
}
