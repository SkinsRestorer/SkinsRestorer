package skinsrestorer.bungee;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.format.SkinProperty;
import skinsrestorer.shared.storage.SkinStorage;

public class SkinFactoryBungee {

	private static Field profileField = null;
	public static SkinFactoryBungee skinfactory;
	public SkinFactoryBungee(){
		skinfactory = this;
		profileField = getProfileField();
	}
	public static SkinFactoryBungee getFactory(){
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
	//Apply the skin to the player.
	public void applySkin(final ProxiedPlayer player){
		 SkinProfile skinprofile = SkinStorage.getInstance().getOrCreateSkinData(player.getName().toLowerCase());
			skinprofile.applySkin(new SkinProfile.ApplyFunction() {
				@Override
				public void applySkin(SkinProperty property) {
					try {
						Property textures = new Property(property.getName(), property.getValue(), property.getSignature());
						InitialHandler handler = (InitialHandler) player.getPendingConnection();
						LoginResult profile = (LoginResult) profileField.get(handler);
						if (profile == null) {
							profile = new LoginResult(player.getUniqueId().toString(), new Property[] { textures });
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
								updatePlayer(player, newprops);
							}
						}
						profileField.set(handler, profile);
						updatePlayer(player, profile.getProperties());
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			});
		}
	
	//TODO Instant skin update
	public void updatePlayer(ProxiedPlayer player, Property[] property){
		UserConnection user = BungeeCord.getInstance().getPlayerByOfflineUUID(player.getUniqueId());
		LoginResult profile = user.getPendingConnection().getLoginProfile();
		profile.setProperties(property);
		user.setPing(player.getPing());
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