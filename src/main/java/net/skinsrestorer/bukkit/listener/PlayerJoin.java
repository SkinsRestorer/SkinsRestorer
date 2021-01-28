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
package net.skinsrestorer.bukkit.listener;

import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.SRLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin implements Listener {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    public PlayerJoin(final SkinsRestorer plugin) {
        this.plugin = plugin;
        log = plugin.getSrLogger();
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent e) {
        if (Config.DISABLE_ONJOIN_SKINS) {
            // factory.applySkin(e.get(), SkinStorage.getSkinData(SkinStorage.getPlayerSkin(e.get().getName())));
            // shouldn't it just skip it if it's true?
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                final SkinStorage skinStorage = plugin.getSkinStorage();
                final Player player = e.getPlayer();
                final String nick = player.getName();

                // Don't change skin if player has no custom skin-name set and his username is invalid
                if (skinStorage.getPlayerSkin(nick) == null && !C.validUsername(nick)) {
                    log.log("Not applying skin to " + nick + " (invalid username).");
                    return;
                }
                final String skin = skinStorage.getDefaultSkinNameIfEnabled(nick);

                if (C.validUrl(skin)) {
                    plugin.getFactory().applySkin(player, plugin.getMineSkinAPI().genSkin(skin));
                } else {
                    plugin.getFactory().applySkin(player, skinStorage.getOrCreateSkinForPlayer(skin));
                }
            } catch (SkinRequestException ignored) {
            }
        });
    }
}
