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
import net.skinsrestorer.shared.api.SharedSkinApplier;
import net.skinsrestorer.shared.codec.SRInputReader;
import net.skinsrestorer.shared.codec.SRServerPluginMessage;
import net.skinsrestorer.shared.listeners.event.SRServerMessageEvent;
import net.skinsrestorer.shared.plugin.SRServerAdapter;
import net.skinsrestorer.shared.utils.SRConstants;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class SRServerMessageAdapter {
    private final SRServerAdapter plugin;
    private final SharedSkinApplier<Object> skinApplier;

    public void handlePluginMessage(SRServerMessageEvent event) {
        if (!event.getChannel().equals(SRConstants.MESSAGE_CHANNEL)) {
            return;
        }

        SRServerPluginMessage message = SRServerPluginMessage.CODEC.read(new SRInputReader(event.getData()));
        SRServerPluginMessage.ChannelPayload<?> channelPayload = message.channelPayload();
        if (channelPayload instanceof SRServerPluginMessage.GUIPageChannelPayload payload) {
            plugin.openGUI(event.getPlayer(), payload.srInventory());
        } else if (channelPayload instanceof SRServerPluginMessage.SkinUpdateChannelPayload payload) {
            skinApplier.applySkin(event.getPlayer().getAs(Object.class), payload.skinProperty());
        }
    }
}
