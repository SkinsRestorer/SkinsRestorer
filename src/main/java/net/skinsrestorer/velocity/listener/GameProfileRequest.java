/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.velocity.listener;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.velocity.SkinsRestorer;

public class GameProfileRequest {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    @Inject
    public GameProfileRequest(SkinsRestorer plugin) {
        this.plugin = plugin;
        log = plugin.getSrLogger();
    }

    @Subscribe
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        String name = event.getUsername();

        if (Config.DISABLE_ONJOIN_SKINS)
            return;

        if (event.isOnlineMode())
            return;

        String skin = plugin.getSkinStorage().getDefaultSkinName(name);

        //todo: default skinurl support
        try {
            event.setGameProfile(plugin.getSkinApplierVelocity().updateProfileSkin(event.getGameProfile(), skin));
        } catch (SkinRequestException ignored) {
        }
    }
}
