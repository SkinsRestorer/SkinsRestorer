package skinsrestorer.velocity.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.velocity.contexts.OnlinePlayer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.util.GameProfile;
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
    @Syntax(" [help]")
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
        source.sendMessage(plugin.deserialize(Locale.DATA_DROPPED.replace("%playerOrSkin", e.name()).replace("%targets", targetList)));
    }


    @Subcommand("props") @CommandPermission("%srProps")
    @CommandCompletion("@players")
    @Description("%helpSrProps")
    @Syntax(" <target>")
    public void onProps(CommandSource source, OnlinePlayer target) {
        GameProfile.Property prop = target.getPlayer().getGameProfileProperties().get(0);

        if (prop == null) {
            source.sendMessage(plugin.deserialize(Locale.NO_SKIN_DATA));
            return;
        }
        byte[] decoded = Base64.getDecoder().decode(prop.getValue());

        String decodedString = new String(decoded);
        JsonObject jsonObject = new JsonParser().parse(decodedString).getAsJsonObject();
        String decodedSkin = jsonObject.getAsJsonObject().get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").toString();
        long timestamp = Long.parseLong(jsonObject.getAsJsonObject().get("timestamp").toString());
        String requestDate = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (timestamp));

        source.sendMessage(plugin.deserialize("§aRequest time: §e" + requestDate));
        source.sendMessage(plugin.deserialize("§aprofileId: §e" + jsonObject.getAsJsonObject().get("profileId").toString()));
        source.sendMessage(plugin.deserialize("§aName: §e" + jsonObject.getAsJsonObject().get("profileName").toString()));
        source.sendMessage(plugin.deserialize("§aSkinTexture: §e" + decodedSkin.substring(1, decodedSkin.length()-1)));
        source.sendMessage(plugin.deserialize("§cMore info in console!"));

        //console
        System.out.println("\n§aName: §8" + prop.getName());
        System.out.println("\n§aValue : §8" + prop.getValue());
        System.out.println("\n§aSignature : §8" + prop.getSignature());
        System.out.println("\n§aValue Decoded: §e" + Arrays.toString(decoded));
    }

    public enum PlayerOrSkin {
        player,
        skin,
    }
}
