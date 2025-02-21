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
package net.skinsrestorer.shared.gui;

import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.codec.SRProxyPluginMessage;
import net.skinsrestorer.shared.listeners.GUIActionListener;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.subjects.SRServerPlayer;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ActionDataCallback {
    private final SRPlatformAdapter adapter;
    private final Injector injector;
    private final SRServerPlugin plugin;

    public void handle(SRServerPlayer player, SRInventory.ClickEventAction action) {
        if (!action.actionChannelPayload().isEmpty()) {
            if (plugin.isProxyMode()) {
                player.sendToMessageChannel(new SRProxyPluginMessage(new SRProxyPluginMessage.GUIActionChannelPayloadList(action.actionChannelPayload())));
            } else {
                adapter.runAsync(() -> injector.getSingleton(GUIActionListener.class).handle(player, action.actionChannelPayload()));
            }
        }

        if (action.closeInventory()) {
            player.closeInventory();
        }
    }
}
