/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.sponge.SkinsRestorer;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent.Auth;
import org.spongepowered.api.profile.GameProfile;

@RequiredArgsConstructor
public class LoginListener implements EventListener<ClientConnectionEvent.Auth> {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    @Override
    public void handle(Auth event) {
        if (event.isCancelled() && Config.NO_SKIN_IF_LOGIN_CANCELED)
            return;

        if (Config.DISABLE_ONJOIN_SKINS)
            return;

        final GameProfile profile = event.getProfile();

        profile.getName().ifPresent(name -> {
            try {
                // TODO: add default skinurl support
                plugin.getSkinApplierSponge().updateProfileSkin(profile, plugin.getSkinStorage().getDefaultSkinName(name));
            } catch (SkinRequestException ignored) {
            }
        });
    }
}
