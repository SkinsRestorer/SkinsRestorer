package skinsrestorer.bungee;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.ReflectionUtil;

public class SkinFactoryBungee {

	private static Field profileField = null;

	public SkinFactoryBungee() {
		try {
			profileField = ReflectionUtil.getPrivateField(InitialHandler.class, "loginProfile");
		} catch (Exception e) {
			System.err.println("Failed to get method handle for initial handel loginProfile field");
			e.printStackTrace();
		}
	}

	// Apply the skin to the player.
	public void applySkin(final ProxiedPlayer player) {

		ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {

			@Override
			public void run() {
				final SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinForPlayer(player.getName());
				skinprofile.applySkin(new SkinProfile.ApplyFunction() {
					@Override
					public void applySkin(SkinProperty property) {
						try {

							Property textures = new Property(property.getName(), property.getValue(),
									property.getSignature());
							InitialHandler handler = (InitialHandler) player.getPendingConnection();

							LoginResult profile = new LoginResult(player.getUniqueId().toString(),
									new Property[] { textures });
							Property[] present = profile.getProperties();
							Property[] newprops = new Property[present.length + 1];
							System.arraycopy(present, 0, newprops, 0, present.length);
							newprops[present.length] = textures;
							profile.getProperties()[0].setName(newprops[0].getName());
							profile.getProperties()[0].setValue(newprops[0].getValue());
							profile.getProperties()[0].setSignature(newprops[0].getSignature());
							profileField.set(handler, profile);
							updateSkin(player);

						} catch (Throwable t) {
							t.printStackTrace();
						}
					}
				});
			}

		});

	}

	// Remove skin from player

	public void removeSkin(ProxiedPlayer player) {
		LoginResult profile = ((UserConnection) player).getPendingConnection().getLoginProfile();
		InitialHandler handler = (InitialHandler) player.getPendingConnection();
		profile.getProperties()[0].setSignature(""); // This should do the
														// trick.
		try {
			profileField.set(handler, profile);
		} catch (Exception e) {
			// Skin removing failed !?
		}
		updateSkin(player); // Removing the skin.
	}

	public void updateSkin(final ProxiedPlayer player) {
		ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
			@Override
			public void run() {
				if (player.getServer() == null)
					return;

				sendUpdateRequest((UserConnection) player);
			}
		});
	}

	public void sendUpdateRequest(UserConnection p) {
		final ByteArrayOutputStream b = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(b);
		try {
			out.writeUTF("ForwardToPlayer");
			out.writeUTF(p.getName());

			p.getServer().sendData("SkinUpdate", b.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}