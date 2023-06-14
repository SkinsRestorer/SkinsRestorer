/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.shared.listeners;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.api.SharedSkinApplier;
import net.skinsrestorer.shared.listeners.event.SRServerMessageEvent;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.plugin.SRServerPlugin;
import net.skinsrestorer.shared.subjects.SRPlayer;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRServerMessageAdapter {
    private final SRServerAdapter<?> plugin;
    private final SharedSkinApplier<Object> skinApplier;

    public void handlePluginMessage(SRServerMessageEvent event) {
        if (!event.getChannel().equals("sr:messagechannel")) {
            return;
        }

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));

        try {
            String subChannel = in.readUTF();

            if (subChannel.equalsIgnoreCase("returnSkinsV2")) {
                Optional<SRPlayer> player = plugin.getPlayer(in.readUTF());
                if (!player.isPresent()) {
                    return;
                }

                int page = in.readInt();

                short len = in.readShort();
                byte[] msgBytes = new byte[len];
                in.readFully(msgBytes);

                Map<String, String> skinList = SRServerPlugin.convertToObjectV2(msgBytes);

                plugin.openProxyGUI(player.get(), page, skinList);
            } else if (subChannel.equalsIgnoreCase("SkinUpdateV2")) {
                skinApplier.applySkin(event.getPlayer().getAs(Object.class),
                        SkinProperty.of(in.readUTF(), in.readUTF()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
