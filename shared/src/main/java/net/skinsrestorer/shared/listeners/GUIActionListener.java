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
package net.skinsrestorer.shared.listeners;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.codec.SRProxyPluginMessage;
import net.skinsrestorer.shared.commands.library.SRCommandManager;
import net.skinsrestorer.shared.gui.SharedGUI;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.storage.GUIStorage;
import net.skinsrestorer.shared.subjects.SRPlayer;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GUIActionListener {
    private final SRPlatformAdapter adapter;
    private final GUIStorage guiStorage;
    private final SharedGUI sharedGUI;
    private final SRCommandManager commandManager;

    public void handle(SRPlayer player, SRProxyPluginMessage.GUIActionChannelPayload actionChannelPayload) {
        SRProxyPluginMessage.GUIActionChannelPayload.GUIActionPayload<?> actionPayload = actionChannelPayload.payload();
        if (actionPayload instanceof SRProxyPluginMessage.GUIActionChannelPayload.OpenPagePayload openPagePayload) {
            adapter.openGUI(player, sharedGUI.createGUIPage(player, guiStorage.getGUIPage(player, openPagePayload.page(), openPagePayload.type())));
        } else if (actionPayload instanceof SRProxyPluginMessage.GUIActionChannelPayload.ClearSkinPayload) {
            commandManager.execute(player, "skin clear");
        } else if (actionPayload instanceof SRProxyPluginMessage.GUIActionChannelPayload.SetSkinPayload setSkinPayload) {
            commandManager.execute(player, "skin set %s".formatted(setSkinPayload.skin()));
        }
    }
}
