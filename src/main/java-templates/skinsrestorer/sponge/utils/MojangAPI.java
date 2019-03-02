package skinsrestorer.sponge.utils;

import com.mojang.authlib.properties.Property;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.profile.property.ProfileProperty;

import java.util.Optional;

public class MojangAPI {

    private static final String uuidurl = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String skinurl = "https://sessionserver.mojang.com/session/minecraft/profile/";
    // private static MojangAPI mojangapi = new MojangAPI();

    public static Optional<ProfileProperty> getSkinProperty(String uuid) {
        Property props = (Property) skinsrestorer.shared.utils.MojangAPI.getSkinProperty(uuid);
        return Optional.<ProfileProperty>of(Sponge.getServer().getGameProfileManager().createProfileProperty("textures", props.getValue(), props.getSignature()));
    }

    public static Optional<String> getUUID(String name) {
        try {
            String uuid = skinsrestorer.shared.utils.MojangAPI.getUUID(name);
            return Optional.<String>of(uuid);
        } catch (skinsrestorer.shared.utils.MojangAPI.SkinRequestException e) {
            return Optional.empty();
        }
        // return Optional.<String>of(output.substring(7, 39));
    }
}