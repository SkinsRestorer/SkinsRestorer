package skinsrestorer.sponge.listeners;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent.Login;
import org.spongepowered.api.profile.property.ProfileProperty;
import skinsrestorer.sponge.SkinsRestorer;
import skinsrestorer.sponge.utils.MojangAPI;

import java.util.Collection;
import java.util.Optional;

public class LoginListener implements EventListener<ClientConnectionEvent.Login> {

    @Override
    public void handle(Login e) {
        String name = e.getTargetUser().getName().toLowerCase();
        Collection<ProfileProperty> props = e.getProfile().getPropertyMap().get("textures");
        String skin = SkinsRestorer.getInstance().getDataRoot().getNode("Players", name).getString();

        if (skin == null || skin.isEmpty())
            skin = name;

        skin = skin.toLowerCase();

        ProfileProperty cachedTextures = null;
        try {
            cachedTextures = Sponge.getServer().getGameProfileManager().createProfileProperty("textures",
                    SkinsRestorer.getInstance().getDataRoot().getNode("Skins", skin, "Value").getString(),
                    SkinsRestorer.getInstance().getDataRoot().getNode("Skins", skin, "Signature").getString());

            props.clear();
            props.add(cachedTextures);
        } catch (Exception ex) {
        }

        if (cachedTextures == null) {

            Optional<String> uid = MojangAPI.getUUID(skin);
            if (!uid.isPresent())
                return;

            Optional<ProfileProperty> textures = MojangAPI.getSkinProperty(uid.get());
            if (!textures.isPresent())
                return;

            cachedTextures = textures.get();

            if (!name.equalsIgnoreCase(skin))
                SkinsRestorer.getInstance().getDataRoot().getNode("Players", name).setValue(skin);
            else
                SkinsRestorer.getInstance().getDataRoot().getNode("Players", name).setValue(null);

            SkinsRestorer.getInstance().getDataRoot().getNode("Skins", skin, "Value")
                    .setValue(textures.get().getValue());
            SkinsRestorer.getInstance().getDataRoot().getNode("Skins", skin, "Signature")
                    .setValue(textures.get().getSignature().get());

            SkinsRestorer.getInstance().saveConfigs();

            props.clear();
            props.add(cachedTextures);
            return;
        }

    }

}