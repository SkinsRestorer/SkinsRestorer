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
package net.skinsrestorer.bukkit.listener;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public class PlayerJoin implements Listener {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        if (Config.DISABLE_ON_JOIN_SKINS)
            return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                final Player player = event.getPlayer();
                final String skin = plugin.getSkinStorage().getDefaultSkinName(player.getName());

                if (C.validUrl(skin)) {
                    plugin.getSkinsRestorerAPI().applySkin(new PlayerWrapper(player), plugin.getMineSkinAPI().genSkin(skin, null, null));
                } else {
                    plugin.getSkinsRestorerAPI().applySkin(new PlayerWrapper(player), skin);
                }
            } catch (SkinRequestException ignored) {
            }
        });
    }
}
