package skinsrestorer.velocity.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.velocity.contexts.OnlinePlayer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.util.GameProfile;
import skinsrestorer.shared.interfaces.ISrCommand;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.utils.ServiceChecker;
import skinsrestorer.velocity.SkinsRestorer;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Created by McLive on 23.02.2019.
 */
@CommandAlias("sr|skinsrestorer") @CommandPermission("%sr")
public class SrCommand extends BaseCommand {
    private final SkinsRestorer plugin;

    public SrCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @HelpCommand
    public void onHelp(CommandSource source, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload") @CommandPermission("%srReload")
    @Description("%helpSrReload")
    public void onReload(CommandSource source) {
        Locale.load(plugin.getConfigPath());
        Config.load(plugin.getConfigPath(), plugin.getClass().getClassLoader().getResourceAsStream("config.yml"));
        source.sendMessage(plugin.deserialize(Locale.RELOAD));
    }


    @Subcommand("status") @CommandPermission("%srStatus")
    @Description("%helpSrStatus")
    public void onStatus(CommandSource source) {
        source.sendMessage(plugin.deserialize("§3----------------------------------------------"));
        source.sendMessage(plugin.deserialize("§7Checking needed services for SR to work properly..."));

        plugin.getService().execute(() -> {
            ServiceChecker checker = new ServiceChecker();
            checker.setMojangAPI(plugin.getMojangAPI());
            checker.checkServices();

            ServiceChecker.ServiceCheckResponse response = checker.getResponse();
            List<String> results = response.getResults();

            for (String result : results) {
                source.sendMessage(plugin.deserialize(result));
            }
            source.sendMessage(plugin.deserialize("§7Working UUID API count: §6" + response.getWorkingUUID()));
            source.sendMessage(plugin.deserialize("§7Working Profile API count: §6" + response.getWorkingProfile()));
            if (response.getWorkingUUID() >= 1 && response.getWorkingProfile() >= 1)
                source.sendMessage(plugin.deserialize("§aThe plugin currently is in a working state."));
            else
                source.sendMessage(plugin.deserialize("§cPlugin currently can't fetch new skins. You might check out our discord at https://discord.me/servers/skinsrestorer"));
            source.sendMessage(plugin.deserialize("§3----------------------------------------------"));
            source.sendMessage(plugin.deserialize("§7SkinsRestorer §6v" + plugin.getVersion()));
            source.sendMessage(plugin.deserialize("§7Server: §6" + plugin.getProxy().getVersion()));
            source.sendMessage(plugin.deserialize("§7BungeeMode: §6Velocity-Plugin"));
            source.sendMessage(plugin.deserialize("§7Finished checking services."));
            source.sendMessage(plugin.deserialize("§3----------------------------------------------"));
        });
    }


    @Subcommand("drop") @CommandPermission("%srDrop")
    @CommandCompletion("@players")
    @Description("%helpSrDrop")
    public void onDrop(CommandSource source, OnlinePlayer target) {
        String player = target.getPlayer().getUsername();
        plugin.getSkinStorage().removeSkinData(player);
        source.sendMessage(plugin.deserialize(Locale.SKIN_DATA_DROPPED.replace("%player", player)));
    }


    @Subcommand("props") @CommandPermission("%srProps")
    @CommandCompletion("@players")
    @Description("%helpSrProps")
    public void onProps(CommandSource source, OnlinePlayer target) {
        GameProfile.Property prop = target.getPlayer().getGameProfileProperties().get(0);

        if (prop == null) {
            source.sendMessage(plugin.deserialize(Locale.NO_SKIN_DATA));
            return;
        }

        source.sendMessage(plugin.deserialize("\n§aName: §8" + prop.getName()));
        source.sendMessage(plugin.deserialize("\n§aValue : §8" + prop.getValue()));
        source.sendMessage(plugin.deserialize("\n§aSignature : §8" + prop.getSignature()));

        byte[] decoded = Base64.getDecoder().decode(prop.getValue());
        source.sendMessage(plugin.deserialize("\n§aValue Decoded: §e" + Arrays.toString(decoded)));

        source.sendMessage(plugin.deserialize("\n§e" + Arrays.toString(decoded)));

        source.sendMessage(plugin.deserialize("§cMore info in console!"));
    }
}
