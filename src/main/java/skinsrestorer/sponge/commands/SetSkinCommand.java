package skinsrestorer.sponge.commands;

import java.util.Collection;
import java.util.Optional;

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

public class SetSkinCommand implements CommandExecutor {
    private SkinsRestorer plugin;

    public SetSkinCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource source, CommandContext args) throws CommandException {
        
        Player p = args.<Player>getOne("player").get();

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

        plugin.getSkinApplier().updatePlayerSkin(p);

        p.sendMessage(Text.builder("Your skin has been changed.").color(TextColors.DARK_GREEN).build());

        return CommandResult.success();
    }
}
