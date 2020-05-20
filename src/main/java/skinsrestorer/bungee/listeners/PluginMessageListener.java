package skinsrestorer.bungee.listeners;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.utils.Property;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Created by McLive on 21.12.2019.
 */
public class PluginMessageListener implements Listener {
    private SkinsRestorer plugin;

    public PluginMessageListener(SkinsRestorer plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) throws IOException {
        if (!e.getTag().equals("sr:messagechannel"))
            return;

        /*if (!(e.getSender() instanceof ProxiedPlayer))
            return;*/

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));

        String subchannel = in.readUTF();

        if (subchannel.equals("getSkins")) {
            String player = in.readUTF();
            int page = in.readInt();
            int skinNumber = 26 * page;
            ProxiedPlayer p = plugin.getProxy().getPlayer(player);

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
        }

        if (subchannel.equals("clearSkin")) {
            String player = in.readUTF();
            ProxiedPlayer p = plugin.getProxy().getPlayer(player);

            plugin.getSkinCommand().onSkinClear(p);
        }

        if (subchannel.equals("setSkin")) {
            String player = in.readUTF();
            String skin = in.readUTF();
            ProxiedPlayer p = plugin.getProxy().getPlayer(player);

            plugin.getSkinCommand().onSkinSet(p, skin);
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

    private static Map<String, Property> convertToObject(byte[] byteArr){
        Map<String, Property> map = new TreeMap<>();
        Property obj = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(byteArr);
            ois = new ObjectInputStream(bis);
            while(bis.available() > 0){
                map = (Map<String, Property>)ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return map;
    }
}
