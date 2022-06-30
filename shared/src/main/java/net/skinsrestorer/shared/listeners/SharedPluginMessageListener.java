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
import net.skinsrestorer.shared.interfaces.ISRProxyPlugin;

import java.io.*;
import java.util.Map;
import java.util.Optional;

public abstract class SharedPluginMessageListener {
    private static byte[] convertToByteArray(Map<String, GenericProperty> map) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        try {
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(map);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteOut.toByteArray();
    }

    public void handlePluginMessage(SRPluginMessageEvent event) throws IOException {
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
                int skinNumber = 25 * page;

                Map<String, GenericProperty> skinsList = plugin.getSkinStorage().getSkinsRaw(skinNumber);

                byte[] ba = convertToByteArray(skinsList);

                ByteArrayOutputStream b = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(b);

                try {
                    out.writeUTF("returnSkins");
                    out.writeUTF(playerName);
                    out.writeInt(page);

                    out.writeShort(ba.length);
                    out.write(ba);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                // Payload may not be larger than 32767 bytes -18 from channel name
                if (b.toByteArray().length > 32749) {
                    plugin.getSrLogger().warning("Byte to long in gui... cancel gui..");
                    break;
                }

                player.sendDataToServer("sr:messagechannel", b.toByteArray());
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
    }

    protected abstract ISRProxyPlugin getPlugin();
}
