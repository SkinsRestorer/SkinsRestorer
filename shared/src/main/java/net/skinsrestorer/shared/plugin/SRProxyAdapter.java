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
package net.skinsrestorer.shared.plugin;

import net.skinsrestorer.shared.gui.SRInventory;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;
import net.skinsrestorer.shared.utils.MessageProtocolUtil;

import java.util.Optional;

public interface SRProxyAdapter extends SRPlatformAdapter {
    Optional<SRProxyPlayer> getPlayer(String name);

    @Override
    default void openGUI(SRPlayer player, SRInventory srInventory) {
        SRProxyPlayer proxyPlayer = (SRProxyPlayer) player;
        proxyPlayer.sendToMessageChannel(out -> {
            out.writeUTF("openGUI");
            byte[] ba = MessageProtocolUtil.convertToByteArray(srInventory);
            out.writeInt(ba.length);
            out.write(ba);
        });
    }
}
