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
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.api.SharedSkinApplier;
import net.skinsrestorer.shared.gui.PageInfo;
import net.skinsrestorer.shared.listeners.event.SRServerMessageEvent;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.utils.MessageProtocolUtil;
import net.skinsrestorer.shared.utils.SRConstants;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRServerMessageAdapter {
    private final SRServerAdapter plugin;
    private final SharedSkinApplier<Object> skinApplier;
    private final SRLogger logger;

    public void handlePluginMessage(SRServerMessageEvent event) {
        if (!event.getChannel().equals(SRConstants.MESSAGE_CHANNEL)) {
            return;
        }

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));

        try {
            String subChannel = in.readUTF();

            if (subChannel.equalsIgnoreCase("returnSkinsV5")) {
                int len = in.readInt();
                byte[] msgBytes = new byte[len];
                in.readFully(msgBytes);

                PageInfo pageInfo = MessageProtocolUtil.convertToPageInfo(msgBytes);

                plugin.openGUIPage(event.getPlayer(), pageInfo);
            } else if (subChannel.equalsIgnoreCase("SkinUpdateV2")) {
                skinApplier.applySkin(event.getPlayer().getAs(Object.class),
                        SkinProperty.of(in.readUTF(), in.readUTF()));
            }
        } catch (IOException e) {
            logger.severe("Error while handling plugin message", e);
        }
    }
}
