package skinsrestorer.bungee;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.ReflectionUtil;

public class SkinFactoryBungee {

	// Apply the skin to the player.
	public void applySkin(final ProxiedPlayer player) {
		ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {

			@Override
			public void run() {
				try {
					SkinProfile sp = SkinStorage.getInstance().getOrCreateSkinForPlayer(player.getName());
					Property textures = new Property(sp.getSkinProperty().getName(), sp.getSkinProperty().getValue(),
							sp.getSkinProperty().getSignature());

					if (player.getPendingConnection().isOnlineMode()) {
						sendUpdateRequest(player, textures);
						return;
					}

					InitialHandler handler = (InitialHandler) player.getPendingConnection();

					LoginResult profile = new LoginResult(player.getUniqueId().toString(), new Property[] { textures });
					Property[] present = profile.getProperties();
					Property[] newprops = new Property[present.length + 1];
					System.arraycopy(present, 0, newprops, 0, present.length);
					newprops[present.length] = textures;
					profile.getProperties()[0].setName(newprops[0].getName());
					profile.getProperties()[0].setValue(newprops[0].getValue());
					profile.getProperties()[0].setSignature(newprops[0].getSignature());
					ReflectionUtil.getPrivateField(InitialHandler.class, "loginProfile").set(handler, profile);
					sendUpdateRequest(player, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	public void sendUpdateRequest(ProxiedPlayer p, Property textures) {
		if (p.getServer() == null)
			return;

		ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);
		try {
			out.writeUTF("ForwardToPlayer");
			out.writeUTF(p.getName());

			if (textures != null) {
				out.writeUTF(textures.getName());
				out.writeUTF(textures.getValue());
				out.writeUTF(textures.getSignature());
			}

			p.getServer().sendData("SkinUpdate", b.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}