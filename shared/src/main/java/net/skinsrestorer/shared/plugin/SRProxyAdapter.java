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

import net.skinsrestorer.shared.codec.SRServerPluginMessage;
import net.skinsrestorer.shared.gui.SRInventory;
import net.skinsrestorer.shared.subjects.SRPlayer;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;

public interface SRProxyAdapter extends SRPlatformAdapter {
    @Override
    default void openGUI(SRPlayer player, SRInventory srInventory) {
        SRProxyPlayer proxyPlayer = (SRProxyPlayer) player;
        proxyPlayer.sendToMessageChannel(new SRServerPluginMessage(new SRServerPluginMessage.GUIPageChannelPayload(srInventory)));
    }

    @Override
    default void giveSkullItem(SRPlayer player, SRServerPluginMessage.GiveSkullChannelPayload giveSkullPayload) {
        SRProxyPlayer proxyPlayer = (SRProxyPlayer) player;
        proxyPlayer.sendToMessageChannel(new SRServerPluginMessage(giveSkullPayload));
    }
}
