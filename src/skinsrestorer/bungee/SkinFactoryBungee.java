package skinsrestorer.bungee;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.Factory;

public class SkinFactoryBungee extends Factory {

	private static Field profileField = null;
	private static SkinFactoryBungee skinfactory;

	public SkinFactoryBungee() {
		profileField = getProfileField();
	}
    @Deprecated
	public static SkinFactoryBungee getFactory() {
		return skinfactory;
	}

	private static Field getProfileField() {
		try {
			Field profileField = InitialHandler.class.getDeclaredField("loginProfile");
			profileField.setAccessible(true);
			return profileField;
		} catch (Throwable t) {
			System.err.println("Failed to get method handle for initial handel loginProfile field");
			t.printStackTrace();
		}
		return null;
	}

	// Apply the skin to the player.
	public void applySkin(final ProxiedPlayer player) {
		ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
			@Override
			public void run() {

				final SkinProfile skinprofile = SkinStorage.getInstance()
						.getOrCreateSkinData(player.getName().toLowerCase());
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
							updateSkin(player, profile);

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
		profile.getProperties()[0].setSignature(""); //This should do the trick.
		try {
			profileField.set(handler, profile);
		} catch (IllegalArgumentException e) {
			// Skin removing failed !?
		} catch (IllegalAccessException e) {
			// Skin removing failed !?
		}
		updateSkin(player, profile); // Removing the skin.
	}

	public void updateSkin(final ProxiedPlayer player, final LoginResult profile) {
		ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
			@Override
			public void run() {

				final UserConnection user = (UserConnection) player;
				String[][] props = new String[profile.getProperties().length][];
				for (int i = 0; i < props.length; i++) {
					props[i] = new String[] { profile.getProperties()[i].getName(),
							profile.getProperties()[i].getValue(), profile.getProperties()[i].getSignature() };
				}
				if (player.getServer() == null) {
					return;
				}
				sendUpdateRequest(user);
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

	// Refletion stuff down there.
	protected static void setValue(Object owner, Field field, Object value) throws Exception {
		makeModifiable(field);
		field.set(owner, value);
	}

	protected static void makeModifiable(Field nameField) throws Exception {
		nameField.setAccessible(true);
		int modifiers = nameField.getModifiers();
		Field modifierField = nameField.getClass().getDeclaredField("modifiers");
		modifiers = modifiers & ~Modifier.FINAL;
		modifierField.setAccessible(true);
		modifierField.setInt(nameField, modifiers);
	}

	@SuppressWarnings("unused")
	private Object getValue(Object instance, String field) throws Exception {
		Field f = instance.getClass().getDeclaredField(field);
		f.setAccessible(true);
		return f.get(instance);
	}
}