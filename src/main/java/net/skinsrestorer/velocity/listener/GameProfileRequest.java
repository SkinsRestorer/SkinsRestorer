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
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.SRLogger;
import net.skinsrestorer.velocity.SkinsRestorer;

public class GameProfileRequest {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    @Inject
    public GameProfileRequest(SkinsRestorer plugin) {
        this.plugin = plugin;
        log = plugin.getLogger();
    }

    @Subscribe
    public void onGameProfileRequest(GameProfileRequestEvent e) {
        String name = e.getUsername();

        if (Config.DISABLE_ONJOIN_SKINS)
            return;

        if (e.isOnlineMode())
            return;

        String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(name);

        //todo: default skinurl support
        try {
            e.setGameProfile(plugin.getSkinApplierVelocity().updateProfileSkin(e.getGameProfile(), skin));
        } catch (SkinRequestException ignored) {
        }
    }
}
