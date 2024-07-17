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
import net.skinsrestorer.shared.gui.CodecHelpers;
import net.skinsrestorer.shared.gui.PageType;
import net.skinsrestorer.shared.gui.SharedGUI;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlatformAdapter;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import net.skinsrestorer.shared.subjects.SRPlayer;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GUIActionListener {
    private final SRLogger logger;
    private final SRPlatformAdapter adapter;
    private final SkinStorageImpl skinStorage;
    private final SharedGUI sharedGUI;
    private final SRCommandManager commandManager;

    public void handle(SRPlayer player, byte[] data) {
        handle(player, new DataInputStream(new ByteArrayInputStream(data)));
    }

    public void handle(SRPlayer player, DataInput in) {
        try {
            String subChannel = in.readUTF();
            switch (subChannel) {
                case "openPage" -> {
                    int page = CodecHelpers.INT_CODEC.read(in);
                    PageType type = PageType.CODEC.read(in);

                    adapter.openGUI(player, sharedGUI.createGUIPage(player, skinStorage.getGUIPage(player, page, type)));
                }
                case "clearSkin" -> commandManager.execute(player, "skin clear");
                case "setSkin" -> {
                    String skin = CodecHelpers.STRING_CODEC.read(in);
                    commandManager.execute(player, String.format("skin set %s", skin));
                }
            }
        } catch (IOException e) {
            logger.severe("Error while handling plugin message", e);
        }
    }
}
