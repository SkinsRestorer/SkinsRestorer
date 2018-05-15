package skinsrestorer.sponge.commands;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.sponge.SkinsRestorer;
import skinsrestorer.sponge.utils.MojangAPI;

import java.util.Collection;
import java.util.Optional;

public class SkinCommand implements CommandExecutor {
    private SkinsRestorer plugin;

    public SkinCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {

        if (!(source instanceof Player)) {
            source.sendMessage(Text.builder("This command is only for players!").color(TextColors.RED).build());
            return CommandResult.empty();
        }
        Player p = (Player) source;
        String name = p.getName().toLowerCase();

        String skin = args.<String>getOne("skin").get().toLowerCase();
        Collection<ProfileProperty> props = p.getProfile().getPropertyMap().get("textures");

        Optional<String> uid = MojangAPI.getUUID(skin);

        if (!uid.isPresent()) {
            p.sendMessage(Text.builder("This player does not exist, or is not premium.").color(TextColors.RED).build());
            return CommandResult.empty();
        }

        Optional<ProfileProperty> textures = MojangAPI.getSkinProperty(uid.get());

        if (!textures.isPresent()) {
            p.sendMessage(
                    Text.builder("Could not get player's skin data (Rate Limited ?).").color(TextColors.RED).build());
            return CommandResult.empty();
        }

        props.clear();
        props.add(textures.get());

        if (!name.equalsIgnoreCase(skin))
            SkinsRestorer.getInstance().getDataRoot().getNode("Players", name).setValue(skin);
        else
            SkinsRestorer.getInstance().getDataRoot().getNode("Players", name).setValue(null);

        SkinsRestorer.getInstance().getDataRoot().getNode("Skins", skin, "Value").setValue(textures.get().getValue());
        SkinsRestorer.getInstance().getDataRoot().getNode("Skins", skin, "Signature")
                .setValue(textures.get().getSignature().get());

        SkinsRestorer.getInstance().saveConfigs();

        plugin.getSkinApplier().updatePlayerSkin(p);

        p.sendMessage(Text.builder("Your skin has been changed.").color(TextColors.DARK_GREEN).build());

        return CommandResult.success();
    }
}
