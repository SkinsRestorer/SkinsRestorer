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
package net.skinsrestorer.shared.subjects;

import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.utils.DataStreamConsumer;
import net.skinsrestorer.shared.utils.MessageProtocolUtil;

import java.util.Map;
import java.util.Optional;

public interface SRProxyPlayer extends SRPlayer {
    Optional<String> getCurrentServer();

    default void sendPage(int page, Map<String, String> skins) {
        sendToMessageChannel(out -> {
            out.writeUTF("returnSkinsV3");
            out.writeUTF(getName());
            out.writeInt(page);

            byte[] ba = MessageProtocolUtil.convertToByteArray(skins);
            out.writeShort(ba.length);
            out.write(ba);
        });
    }

    default void sendUpdateRequest(SkinProperty textures) {
        sendToMessageChannel(out -> {
            out.writeUTF("SkinUpdateV2");

            if (textures != null) {
                out.writeUTF(textures.getValue());
                out.writeUTF(textures.getSignature());
            }
        });
    }

    default void sendToMessageChannel(DataStreamConsumer consumer) {
        sendToMessageChannel(consumer.toByteArray());
    }

    void sendToMessageChannel(byte[] data);
}
