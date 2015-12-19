package skinsrestorer.bungee.listeners;

import java.lang.reflect.Field;

import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class LoginListener implements Listener {

	private static final Field profileField = getProfileField();
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

	//load skin data on login
	@EventHandler(priority = EventPriority.LOW)
	public void onPreLogin(final LoginEvent event) {
		if (event.isCancelled()) {
			return;
		}
		final String name = event.getConnection().getName();
		final SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinData(name.toLowerCase());
		event.registerIntent(SkinsRestorer.getInstance());
		ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
			@Override
			public void run() {
				try {
					skinprofile.attemptUpdateBungee();
				} catch (SkinFetchFailedException e) {
					SkinsRestorer.getInstance().logInfo("Skin fetch failed for player " + name + ": " + e.getMessage());
				} finally {
					event.completeIntent(SkinsRestorer.getInstance());
				}
			}
		});
	}

	//fix profile on login
	@EventHandler(priority = EventPriority.LOW)
	public void onPostLogin(final PostLoginEvent event) {
		final String name = event.getPlayer().getName();
		ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
			@Override
			public void run() {
		SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinData(name.toLowerCase());
		skinprofile.applySkin(new SkinProfile.ApplyFunction() {
			@Override
			public void applySkin(SkinProperty property) {
				try {
					Property textures = new Property(property.getName(), property.getValue(), property.getSignature());
					InitialHandler handler = (InitialHandler) event.getPlayer().getPendingConnection();
					LoginResult profile = (LoginResult) profileField.get(handler);
					if (profile == null) {
						profile = new LoginResult(event.getPlayer().getUniqueId().toString(), new Property[] { textures });
					} else {
						Property[] present = profile.getProperties();
						boolean alreadyHasSkin = false;
						for (Property prop : present) {
							if (prop.getName().equals(textures.getName())) {
								alreadyHasSkin = true;
							}
						}
						if (!alreadyHasSkin) {
							Property[] newprops = new Property[present.length + 1];
							System.arraycopy(present, 0, newprops, 0, present.length);
							newprops[present.length] = textures;
							profile.setProperties(newprops);
						}
					}
					profileField.set(handler, profile);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
	}
		});
}
}
