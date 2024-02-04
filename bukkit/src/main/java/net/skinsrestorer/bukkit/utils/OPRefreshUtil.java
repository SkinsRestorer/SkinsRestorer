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
package net.skinsrestorer.bukkit.utils;

import net.skinsrestorer.bukkit.SRBukkitAdapter;
import net.skinsrestorer.shared.utils.CodeComment;
import org.bukkit.entity.Player;

/**
 * The OPRefreshUtil class provides a method to refresh the OP status of a player.
 * This method is used to fix a bug where changing a player's skin causes CommandBlocks to no longer work.
 * It is important to note that this method is NOT AN OP EXPLOIT, despite potentially being falsely reported by anti-malware plugins.
 * This code is being used by PaperMC in their server software as well.
 *
 * @see SRBukkitAdapter
 */
public class OPRefreshUtil {
    @CodeComment({
            "This method is used to refresh the OP status of a player.",
            "This is used to fix a bug where changing your skin causes command blocks to no longer work.",
            "This might be FALSELY REPORTED by anti-malware plugins and this is NOT A OP EXPLOIT.",
            "This code is being used by PaperMC in their server software as well: https://github.com/PaperMC/Paper/blob/eb8f2bb2a183746381738b5ccf08d209efa542b8/patches/server/0181-Player.setPlayerProfile-API.patch#L193-L196"
    })
    public static void refreshOP(Player player, SRBukkitAdapter adapter) {
        // Here we check if the player is /OP
        if (player.isOp()) {
            adapter.runSyncToPlayer(player, () -> {
                // Here we /deOP and /OP the same player
                player.setOp(false);
                player.setOp(true);
            });
        }
    }
}
