package skinsrestorer.sponge.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.sponge.contexts.OnlinePlayer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.profile.property.ProfileProperty;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.ServiceChecker;
import skinsrestorer.sponge.SkinsRestorer;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

/**
 * Created by McLive on 28.02.2019.
 */
@CommandAlias("sr|skinsrestorer") @CommandPermission("%sr")
public class SrCommand extends BaseCommand {
    private final SkinsRestorer plugin;

    public SrCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @HelpCommand
    public static void onHelp(CommandSource source, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload") @CommandPermission("%srReload")
    @Description("%helpSrReload")
    public void onReload(CommandSource source) {
        Locale.load(plugin.getConfigPath());
        Config.load(plugin.getConfigPath(), plugin.getClass().getClassLoader().getResourceAsStream("config.yml"));
        source.sendMessage(plugin.parseMessage(Locale.RELOAD));
    }


    @Subcommand("status") @CommandPermission("%srStatus")
    @Description("%helpSrStatus")
    public void onStatus(CommandSource source) {
        source.sendMessage(plugin.parseMessage("Checking needed services for SR to work properly..."));

        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            ServiceChecker checker = new ServiceChecker();
            checker.checkServices();

            ServiceChecker.ServiceCheckResponse response = checker.getResponse();
            List<String> results = response.getResults();

            for (String result : results) {
                source.sendMessage(plugin.parseMessage(result));
            }
            source.sendMessage(plugin.parseMessage("Working UUID API count: " + response.getWorkingUUID()));
            source.sendMessage(plugin.parseMessage("Working Profile API count: " + response.getWorkingProfile()));
            if (response.getWorkingUUID() >= 1 && response.getWorkingProfile() >= 1)
                source.sendMessage(plugin.parseMessage("The plugin currently is in a working state."));
            else
                source.sendMessage(plugin.parseMessage("Plugin currently can't fetch new skins. You might check out our discord at https://discordapp.com/invite/012gnzKK9EortH0v2?utm_source=Discord%20Widget&utm_medium=Connect"));
            source.sendMessage(plugin.parseMessage("Finished checking services."));
        });
    }


    @Subcommand("drop") @CommandPermission("%srDrop")
    @CommandCompletion("@players")
    @Description("%helpSrDrop")
    public void onDrop(CommandSource source, OnlinePlayer target) {
        String player = target.getPlayer().getName();
        SkinStorage.removeSkinData(player);
        source.sendMessage(plugin.parseMessage(Locale.SKIN_DATA_DROPPED.replace("%player", player)));
    }


    @Subcommand("props") @CommandPermission("%srProps")
    @CommandCompletion("@players")
    @Description("%helpSrProps")
    public void onProps(CommandSource source, OnlinePlayer target) {
        Collection<ProfileProperty> prop = target.getPlayer().getProfile().getPropertyMap().get("textures");

        if (prop == null) {
            source.sendMessage(plugin.parseMessage(Locale.NO_SKIN_DATA));
            return;
        }

        prop.forEach(profileProperty -> {
            source.sendMessage(plugin.parseMessage("\n§aName: §8" + profileProperty.getName()));
            source.sendMessage(plugin.parseMessage("\n§aValue : §8" + profileProperty.getValue()));
            source.sendMessage(plugin.parseMessage("\n§aSignature : §8" + profileProperty.getSignature()));

            byte[] decoded = Base64.getDecoder().decode(profileProperty.getValue());
            source.sendMessage(plugin.parseMessage("\n§aValue Decoded: §e" + Arrays.toString(decoded)));

            source.sendMessage(plugin.parseMessage("\n§e" + Arrays.toString(decoded)));

            source.sendMessage(plugin.parseMessage("§cMore info in console!"));
        });
    }
}
