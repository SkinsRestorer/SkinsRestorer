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
package net.skinsrestorer.shared.listeners;

import net.skinsrestorer.api.interfaces.ISRProxyPlayer;
import net.skinsrestorer.api.property.GenericProperty;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.interfaces.ISRProxyPlugin;

import java.io.*;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

public abstract class SharedPluginMessageListener {
    private static byte[] convertToByteArray(Map<String, String> map) {
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

    public void handlePluginMessage(SRPluginMessageEvent event) {
        ISRProxyPlugin plugin = getPlugin();

        if (event.isCancelled())
            return;

        if (!event.getTag().equals("sr:messagechannel") && !event.getTag().equals("sr:skinchange"))
            return;

        if (!event.isServerConnection()) {
            event.setCancelled(true);
            return;
        }

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));

        try {
            String subChannel = in.readUTF();
            String playerName = in.readUTF();
            Optional<ISRProxyPlayer> optional = plugin.getPlayer(playerName);

            if (!optional.isPresent())
                return;

            ISRProxyPlayer player = optional.get();

            switch (subChannel) {
                //sr:messagechannel
                case "getSkins":
                    int page = in.readInt();
                    if (page > 999)
                        page = 999;
                    int skinNumber = 36 * page;

                    byte[] ba = convertToByteArray(plugin.getSkinStorage().getSkins(skinNumber));

                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(b);

                    try {
                        out.writeUTF("returnSkinsV2");
                        out.writeUTF(playerName);
                        out.writeInt(page);

                        out.writeShort(ba.length);
                        out.write(ba);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    byte[] data = b.toByteArray();
                    plugin.getSrLogger().debug("Sending skins to " + playerName + " (" + data.length + " bytes)");
                    // Payload may not be larger than 32767 bytes -18 from channel name
                    if (data.length > 32749) {
                        plugin.getSrLogger().warning("Too many bytes GUI... canceling GUI..");
                        break;
                    }

                    player.sendDataToServer("sr:messagechannel", data);
                    break;
                case "clearSkin":
                    plugin.getSkinCommand().onSkinClearOther(player, player);
                    break;
                case "updateSkin":
                    plugin.getSkinCommand().onSkinUpdateOther(player, player);
                    break;
                case "setSkin":
                    String skin = in.readUTF();
                    plugin.getSkinCommand().onSkinSetOther(player, player, skin, null);
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract ISRProxyPlugin getPlugin();
}
