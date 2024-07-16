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
import net.skinsrestorer.shared.commands.library.SRCommandManager;
import net.skinsrestorer.shared.gui.PageType;
import net.skinsrestorer.shared.listeners.event.SRProxyMessageEvent;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.utils.SRConstants;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRProxyMessageAdapter {
    private final SkinStorageImpl skinStorage;
    private final SRCommandManager commandManager;
    private final SRLogger logger;

    public void handlePluginMessage(SRProxyMessageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!event.getChannel().equals(SRConstants.MESSAGE_CHANNEL)) {
            return;
        }

        if (!event.isSenderServerConnection() || !event.isReceiverProxyPlayer()) {
            event.setCancelled(true);
            return;
        }

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
        try {
            String subChannel = in.readUTF();
            switch (subChannel) {
                case "getSkinsV3" -> event.getPlayer().sendPageToServer(skinStorage.getGUIPage(event.getPlayer(), in.readInt(), PageType.fromKey(in.readUTF()).orElseThrow()));
                case "clearSkinV2" -> commandManager.execute(event.getPlayer(), "skin clear");
                case "setSkinV2" -> {
                    String skin = in.readUTF();
                    commandManager.execute(event.getPlayer(), String.format("skin set %s", skin));
                }
            }
        } catch (IOException e) {
            logger.severe("Error while handling plugin message", e);
        }
    }
}
