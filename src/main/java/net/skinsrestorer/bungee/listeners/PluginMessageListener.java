/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.bungee.listeners;

import co.aikar.commands.bungee.contexts.OnlinePlayer;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.skinsrestorer.bungee.SkinsRestorer;
import net.skinsrestorer.shared.utils.Property;

import java.io.*;
import java.util.Map;

public class PluginMessageListener implements Listener {
    private final SkinsRestorer plugin;

    public PluginMessageListener(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) throws IOException {
        if (e.isCancelled())
            return;

        if (!e.getTag().equals("sr:messagechannel") && !e.getTag().equals("sr:skinchange"))
            return;

        if (!(e.getSender() instanceof ServerConnection)) {
            e.setCancelled(true);
            return;
        }

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));

        String subChannel = in.readUTF();
        String player = in.readUTF();
        ProxiedPlayer p = plugin.getProxy().getPlayer(player);

        switch (subChannel) {
            //sr:messagechannel
            case "getSkins":
                int page = in.readInt();
                if (page > 999)
                    page = 999;
                int skinNumber = 26 * page;

                Map<String, Property> skinsList = plugin.getSkinStorage().getSkinsRaw(skinNumber);

                byte[] ba = convertToByteArray(skinsList);

                ByteArrayOutputStream b = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(b);

                try {
                    out.writeUTF("returnSkins");
                    out.writeUTF(player);
                    out.writeInt(page);

                    out.writeShort(ba.length);
                    out.write(ba);

                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                p.getServer().sendData("sr:messagechannel", b.toByteArray());
                break;
            case "clearSkin":
                plugin.getSkinCommand().onSkinClearOther(p, new OnlinePlayer(p));
                break;
            case "updateSkin":
                plugin.getSkinCommand().onSkinUpdateOther(p, new OnlinePlayer(p));
                break;
            case "setSkin":
                String skin = in.readUTF();
                plugin.getSkinCommand().onSkinSetOther(p, new OnlinePlayer(p), skin);
                break;
            default:
                break;
        }
    }

    public void sendGuiOpenRequest(ProxiedPlayer p) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);

        try {
            out.writeUTF("OPENGUI");
            out.writeUTF(p.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }

        p.getServer().sendData("sr:messagechannel", b.toByteArray());
    }

    private static byte[] convertToByteArray(Map<String, Property> map) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        try {
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(map);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteOut.toByteArray();
    }
}
