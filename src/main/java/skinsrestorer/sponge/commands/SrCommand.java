package skinsrestorer.sponge.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.sponge.contexts.OnlinePlayer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.profile.property.ProfileProperty;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
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
    @Syntax(" [help]")
    public void onHelp(CommandSource source, CommandHelp help) {
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
        source.sendMessage(plugin.parseMessage("§3----------------------------------------------"));
        source.sendMessage(plugin.parseMessage("§7Checking needed services for SR to work properly..."));

        Sponge.getScheduler().createAsyncExecutor(plugin).execute(() -> {
            ServiceChecker checker = new ServiceChecker();
            checker.setMojangAPI(plugin.getMojangAPI());
            checker.checkServices();

            ServiceChecker.ServiceCheckResponse response = checker.getResponse();
            List<String> results = response.getResults();

            for (String result : results) {
                source.sendMessage(plugin.parseMessage(result));
            }
            source.sendMessage(plugin.parseMessage("§7Working UUID API count: §6" + response.getWorkingUUID()));
            source.sendMessage(plugin.parseMessage("§7Working Profile API count: §6" + response.getWorkingProfile()));
            if (response.getWorkingUUID() >= 1 && response.getWorkingProfile() >= 1)
                source.sendMessage(plugin.parseMessage("§aThe plugin currently is in a working state."));
            else
                source.sendMessage(plugin.parseMessage("§cPlugin currently can't fetch new skins. You might check out our discord at https://discord.me/servers/skinsrestorer"));
            source.sendMessage(plugin.parseMessage("§3----------------------------------------------"));
            source.sendMessage(plugin.parseMessage("§7SkinsRestorer §6v" + plugin.getVersion()));
            source.sendMessage(plugin.parseMessage("§7Server: §6" + Sponge.getGame().getPlatform().getMinecraftVersion()));
            source.sendMessage(plugin.parseMessage("§7BungeeMode: §6Sponge-Plugin"));
            source.sendMessage(plugin.parseMessage("§7Finished checking services."));
            source.sendMessage(plugin.parseMessage("§3----------------------------------------------"));
        });
    }


    @Subcommand("drop|remove") @CommandPermission("%srDrop")
    @CommandCompletion("player|skin @players")
    @Description("%helpSrDrop")
    @Syntax(" <player|skin> <target> [target2]")
    public void onDrop(CommandSource source, PlayerOrSkin e, String[] targets) {
        if (e.name().equalsIgnoreCase("player"))
            for (String targetPlayer : targets)
                plugin.getSkinStorage().removePlayerSkin(targetPlayer);
        else
            for (String targetSkin : targets)
                plugin.getSkinStorage().removeSkinData(targetSkin);
        String targetList = Arrays.toString(targets).substring(1, Arrays.toString(targets).length()-1);
        source.sendMessage(plugin.parseMessage(Locale.DATA_DROPPED.replace("%playerOrSkin", e.name()).replace("%targets", targetList)));
    }


    @Subcommand("props") @CommandPermission("%srProps")
    @CommandCompletion("@players")
    @Description("%helpSrProps")
    @Syntax(" <target>")
    public void onProps(CommandSource source, OnlinePlayer target) {
        Collection<ProfileProperty> prop = target.getPlayer().getProfile().getPropertyMap().get("textures");

        if (prop == null) {
            source.sendMessage(plugin.parseMessage(Locale.NO_SKIN_DATA));
            return;
        }

        prop.forEach(profileProperty -> {
            byte[] decoded = Base64.getDecoder().decode(profileProperty.getValue());

            String decodedString = new String(decoded);
            JsonObject jsonObject = new JsonParser().parse(decodedString).getAsJsonObject();
            String decodedSkin = jsonObject.getAsJsonObject().get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").toString();
            long timestamp = Long.parseLong(jsonObject.getAsJsonObject().get("timestamp").toString());
            String requestDate = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (timestamp));

            ConsoleSource console = Sponge.getServer().getConsole();

            source.sendMessage(plugin.parseMessage("§aRequest time: §e" + requestDate));
            source.sendMessage(plugin.parseMessage("§aprofileId: §e" + jsonObject.getAsJsonObject().get("profileId").toString()));
            source.sendMessage(plugin.parseMessage("§aName: §e" + jsonObject.getAsJsonObject().get("profileName").toString()));
            source.sendMessage(plugin.parseMessage("§aSkinTexture: §e" + decodedSkin.substring(1, decodedSkin.length()-1)));
            source.sendMessage(plugin.parseMessage("§cMore info in console!"));

            //Console
            console.sendMessage(plugin.parseMessage("\n§aName: §8" + profileProperty.getName()));
            console.sendMessage(plugin.parseMessage("\n§aValue : §8" + profileProperty.getValue()));
            console.sendMessage(plugin.parseMessage("\n§aSignature : §8" + profileProperty.getSignature()));
            console.sendMessage(plugin.parseMessage("\n§aValue Decoded: §e" + Arrays.toString(decoded)));
        });
    }

    public enum PlayerOrSkin {
        player,
        skin,
    }
}
