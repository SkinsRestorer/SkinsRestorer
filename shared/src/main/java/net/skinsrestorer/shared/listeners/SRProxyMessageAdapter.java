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
import net.skinsrestorer.shared.commands.library.CommandManager;
import net.skinsrestorer.shared.listeners.event.SRProxyMessageEvent;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRProxyAdapter;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRProxyPlayer;
import net.skinsrestorer.shared.utils.SRConstants;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRProxyMessageAdapter {
    private final SkinStorageImpl skinStorage;
    private final SRProxyAdapter<?, ?> plugin;
    private final CommandManager<SRCommandSender> commandManager;
    private final SRLogger logger;

    public void handlePluginMessage(SRProxyMessageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!event.getChannel().equals(SRConstants.MESSAGE_CHANNEL)) {
            return;
        }

        if (!event.isServerConnection()) {
            event.setCancelled(true);
            return;
        }

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
        try {
            String subChannel = in.readUTF();
            Optional<SRProxyPlayer> optional = plugin.getPlayer(in.readUTF());

            if (optional.isEmpty()) {
                return;
            }

            SRProxyPlayer player = optional.get();
            switch (subChannel) {
                case "getSkins" -> {
                    int page = Math.min(in.readInt(), 999);
                    player.sendPage(page, skinStorage.getGUISkins(page * 36));
                }
                case "clearSkin" -> commandManager.executeCommand(player, "skin clear");
                case "setSkin" -> {
                    String skin = in.readUTF();
                    commandManager.executeCommand(player, String.format("skin set %s", skin));
                }
            }
        } catch (IOException e) {
            logger.severe("Error while handling plugin message", e);
        }
    }
}
