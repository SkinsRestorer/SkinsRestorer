package skinsrestorer.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.ReflectionUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class SkinApplier {

    private static Class<?> LoginResult;

    public static void applySkin(final ProxiedPlayer p, final String nick, InitialHandler handler) {
        try {
            String uuid = null;
            if (p == null && handler == null)
                return;

            if (p != null) {
                uuid = p.getUniqueId().toString();
                handler = (InitialHandler) p.getPendingConnection();
            } else {
                uuid = MojangAPI.getUUID(nick);
            }

            Property textures = (Property) SkinStorage.getOrCreateSkinForPlayer(nick);

            if (handler.isOnlineMode()) {
                if (p != null) {
                    sendUpdateRequest(p, textures);
                    return;
                }
            } else {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8)).toString();
            }
            LoginResult profile;

            try {
                // NEW BUNGEECORD
                profile = (net.md_5.bungee.connection.LoginResult) ReflectionUtil.invokeConstructor(LoginResult,
                        new Class<?>[]{String.class, String.class, Property[].class},
                        uuid, nick, new Property[]{textures});
            } catch (Exception e) {
                // FALL BACK TO OLD
                profile = (net.md_5.bungee.connection.LoginResult) ReflectionUtil.invokeConstructor(LoginResult,
                        new Class<?>[]{String.class, Property[].class}, uuid,
                        new Property[]{textures});
            }
            Property[] present = profile.getProperties();
            Property[] newprops = new Property[present.length + 1];
            System.arraycopy(present, 0, newprops, 0, present.length);
            newprops[present.length] = textures;
            profile.getProperties()[0].setName(newprops[0].getName());
            profile.getProperties()[0].setValue(newprops[0].getValue());
            profile.getProperties()[0].setSignature(newprops[0].getSignature());
            // System.out.println("Profile:");
            // System.out.println(profile.getProperties()[0]);
            ReflectionUtil.setObject(InitialHandler.class, handler, "loginProfile", profile);

            if (SkinsRestorer.getInstance().isMultiBungee()) {
                if (p != null)
                    sendUpdateRequest(p, textures);
            } else {
                if (p != null)
                    sendUpdateRequest(p, null);
            }
        } catch (Exception ignored) {
        }
    }

    public static void applySkin(final String pname) {
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(pname);
        applySkin(p, pname, null);
    }

    public static void applySkin(final ProxiedPlayer p) {
        applySkin(p, p.getName(), null);
    }

    public static void init() {
        try {
            LoginResult = ReflectionUtil.getBungeeClass("connection", "LoginResult");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendUpdateRequest(ProxiedPlayer p, Property textures) {
        if (p == null)
            return;

        if (p.getServer() == null)
            return;

        System.out.println("[SkinsRestorer] Sending skin update request for " + p.getName());

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("SkinUpdate");

            if (textures != null) {
                out.writeUTF(textures.getName());
                out.writeUTF(textures.getValue());
                out.writeUTF(textures.getSignature());
            }

            p.getServer().sendData("SkinsRestorer", b.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}