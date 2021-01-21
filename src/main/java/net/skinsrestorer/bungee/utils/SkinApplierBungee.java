package net.skinsrestorer.bungee.utils;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;
import net.skinsrestorer.bungee.SkinsRestorer;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import net.skinsrestorer.shared.utils.SRLogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SkinApplierBungee {
    private final SkinsRestorer plugin;
    private final SRLogger log;
    private static Class<?> loginResult;

    public SkinApplierBungee(SkinsRestorer plugin) {
        this.plugin = plugin;
        this.log = plugin.getSrLogger();
    }

    public void applySkin(final ProxiedPlayer p, final String nick, InitialHandler handler) throws Exception {
        if (p == null && handler == null)
            return;

        if (p != null) {
            handler = (InitialHandler) p.getPendingConnection();
        }
        Property textures = (Property) plugin.getSkinStorage().getOrCreateSkinForPlayer(nick);

        if (handler.isOnlineMode()) {
            if (p != null) {
                sendUpdateRequest(p, textures);
                return;
            }
        }

        LoginResult profile = handler.getLoginProfile();

        if (profile == null) {
            try {
                // NEW BUNGEECORD (id, name, property)
                profile = new LoginResult(null, null, new Property[]{textures});
            } catch (Exception error) {
                // FALL BACK TO OLD (id, property)
                profile = (net.md_5.bungee.connection.LoginResult) ReflectionUtil.invokeConstructor(loginResult,
                        new Class<?>[]{String.class, Property[].class},
                        null, new Property[]{textures});
            }
        }

        Property[] newprops = new Property[]{textures};

        profile.setProperties(newprops);
        ReflectionUtil.setObject(InitialHandler.class, handler, "loginProfile", profile);

        if (SkinsRestorer.getInstance().isMultiBungee()) {
            if (p != null)
                sendUpdateRequest(p, textures);
        } else {
            if (p != null)
                sendUpdateRequest(p, null);
        }
    }

    public void applySkin(final String pname) throws Exception {
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(pname);
        applySkin(p, pname, null);
    }

    public void applySkin(final ProxiedPlayer p) throws Exception {
        applySkin(p, p.getName(), null);
    }

    public static void init() {
        try {
            loginResult = ReflectionUtil.getBungeeClass("connection", "LoginResult");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendUpdateRequest(ProxiedPlayer p, Property textures) {
        if (p == null)
            return;

        if (p.getServer() == null)
            return;

        log.log("[SkinsRestorer] Sending skin update request for " + p.getName());

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("SkinUpdate");

            if (textures != null) {
                out.writeUTF(textures.getName());
                out.writeUTF(textures.getValue());
                out.writeUTF(textures.getSignature());
            }

            p.getServer().sendData("sr:skinchange", b.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
