/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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
package net.skinsrestorer.shared.plugin;

import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.interfaces.SRProxyPlayer;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.storage.SkinStorageImpl;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRProxyPlugin {
    private final SRLogger logger;
    private final SRPlugin plugin;

    public static byte[] convertToByteArray(Map<String, String> map) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        try {
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut)) {
                ObjectOutputStream out = new ObjectOutputStream(gzipOut);
                out.writeObject(map);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteOut.toByteArray();
    }

    public void sendPage(int page, SRProxyPlayer player, SkinStorageImpl skinStorage) {
        int skinNumber = 36 * page;

        byte[] ba = convertToByteArray(skinStorage.getSkins(skinNumber));

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);

        try {
            out.writeUTF("returnSkinsV2");
            out.writeUTF(player.getName());
            out.writeInt(page);

            out.writeShort(ba.length);
            out.write(ba);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        byte[] data = b.toByteArray();
        logger.debug(String.format("Sending skins to %s (%d bytes)", player.getName(), data.length));
        // Payload may not be larger than 32767 bytes -18 from channel name
        if (data.length > 32_749) {
            logger.warning("Too many bytes GUI... canceling GUI..");
            return;
        }

        player.sendDataToServer("sr:messagechannel", data);
    }

    public void sendUpdateRequest(@NotNull SRProxyPlayer player, SkinProperty textures) {
        if (!player.getCurrentServer().isPresent()) {
            return;
        }

        logger.debug("Sending skin update request for " + player.getName());

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("SkinUpdateV2");

            if (textures != null) {
                out.writeUTF(textures.getValue());
                out.writeUTF(textures.getSignature());
            }

            player.sendDataToServer("sr:messagechannel", b.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startupPlatform(SRProxyPlatformInit init) throws InitializeException {
        // Init storage
        plugin.initStorage();

        // Init API
        plugin.registerAPI();

        // Init listener
        init.initLoginProfileListener();
        init.initConnectListener();

        // Init commands
        plugin.initCommands();

        init.initMessageChannel();
    }
}
