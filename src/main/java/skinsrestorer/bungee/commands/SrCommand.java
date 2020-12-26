package skinsrestorer.bungee.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bungee.contexts.OnlinePlayer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.utils.ServiceChecker;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@CommandAlias("sr|skinsrestorer") @CommandPermission("%sr")
public class SrCommand extends BaseCommand {
    private SkinsRestorer plugin;

    public SrCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @HelpCommand
    @Syntax(" [help]")
    public void onHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload") @CommandPermission("%srReload")
    @Description("%helpSrReload")
    public void onReload(CommandSender sender) {
        Locale.load(SkinsRestorer.getInstance().getConfigPath());
        Config.load(SkinsRestorer.getInstance().getConfigPath(), SkinsRestorer.getInstance().getResourceAsStream("config.yml"));
        sender.sendMessage(TextComponent.fromLegacyText(Locale.RELOAD));
    }


    @Subcommand("status") @CommandPermission("%srStatus")
    @Description("%helpSrStatus")
    public void onStatus(CommandSender sender) {
        sender.sendMessage(TextComponent.fromLegacyText("§3----------------------------------------------"));
        sender.sendMessage(TextComponent.fromLegacyText("§7Checking needed services for SR to work properly..."));

        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
            ServiceChecker checker = new ServiceChecker();
            checker.setMojangAPI(plugin.getMojangAPI());
            checker.checkServices();

            ServiceChecker.ServiceCheckResponse response = checker.getResponse();
            List<String> results = response.getResults();

            for (String result : results) {
                sender.sendMessage(TextComponent.fromLegacyText(result));
            }
            sender.sendMessage(TextComponent.fromLegacyText("§7Working UUID API count: §6 " + response.getWorkingUUID()));
            sender.sendMessage(TextComponent.fromLegacyText("§7Working Profile API count: §6" + response.getWorkingProfile()));
            if (response.getWorkingUUID() >= 1 && response.getWorkingProfile() >= 1)
                sender.sendMessage(TextComponent.fromLegacyText("§aThe plugin currently is in a working state."));
            else
                sender.sendMessage(TextComponent.fromLegacyText("§cPlugin currently can't fetch new skins. You might check out our discord at https://discord.me/servers/skinsrestorer"));
            sender.sendMessage(TextComponent.fromLegacyText("§3----------------------------------------------"));
            sender.sendMessage(TextComponent.fromLegacyText("§7SkinsRestorer §6v" + plugin.getVersion()));
            sender.sendMessage(TextComponent.fromLegacyText("§7Server: §6" + plugin.getProxy().getVersion()));
            sender.sendMessage(TextComponent.fromLegacyText("§7BungeeMode: §6Bungee-Plugin"));
            sender.sendMessage(TextComponent.fromLegacyText("§7Finished checking services."));
            sender.sendMessage(TextComponent.fromLegacyText("§3----------------------------------------------"));
        });
    }


    @Subcommand("drop|remove") @CommandPermission("%srDrop")
    @CommandCompletion("player|skin @players")
    @Description("%helpSrDrop")
    @Syntax(" <player|skin> <target> [target2]")
    public void onDrop(CommandSender sender, PlayerOrSkin e, String[] targets) {
        if (e.name().equalsIgnoreCase("player"))
            for (String targetPlayer : targets)
                plugin.getSkinStorage().removePlayerSkin(targetPlayer);
        else
            for (String targetSkin : targets)
                plugin.getSkinStorage().removeSkinData(targetSkin);
        String targetList = Arrays.toString(targets).substring(1, Arrays.toString(targets).length()-1);
        sender.sendMessage(TextComponent.fromLegacyText(Locale.DATA_DROPPED.replace("%playerOrSkin", e.name()).replace("%targets", targetList)));
    }


    @Subcommand("props") @CommandPermission("%srProps")
    @CommandCompletion("@players")
    @Description("%helpSrProps")
    @Syntax(" <target>")
    public void onProps(CommandSender sender, OnlinePlayer target) {
        InitialHandler h = (InitialHandler) target.getPlayer().getPendingConnection();
        LoginResult.Property prop = h.getLoginProfile().getProperties()[0];



        if (prop == null) {
            sender.sendMessage(TextComponent.fromLegacyText(Locale.NO_SKIN_DATA));
            return;
        }
        //decode

        byte[] decoded = Base64.getDecoder().decode(prop.getValue());
        String decodedString = new String(decoded);
        JsonObject jsonObject = new JsonParser().parse(decodedString).getAsJsonObject();
        String decodedSkin = jsonObject.getAsJsonObject().get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").toString();
        long timestamp = Long.parseLong(jsonObject.getAsJsonObject().get("timestamp").toString());
        String requestDate = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (timestamp));

        CommandSender console = BungeeCord.getInstance().getConsole();

        sender.sendMessage(TextComponent.fromLegacyText("§aRequest time: §e" + requestDate));
        sender.sendMessage(TextComponent.fromLegacyText("§aprofileId: §e" + jsonObject.getAsJsonObject().get("profileId").toString()));
        sender.sendMessage(TextComponent.fromLegacyText("§aName: §e" + jsonObject.getAsJsonObject().get("profileName").toString()));
        sender.sendMessage(TextComponent.fromLegacyText("§aSkinTexture: §e" + decodedSkin.substring(1, decodedSkin.length()-1)));
        sender.sendMessage(TextComponent.fromLegacyText("§cMore info in console!"));

        //console message
        console.sendMessage(TextComponent.fromLegacyText("\n§aName: §8" + prop.getName()));
        console.sendMessage(TextComponent.fromLegacyText("\n§aValue : §8" + prop.getValue()));
        console.sendMessage(TextComponent.fromLegacyText("\n§aSignature : §8" + prop.getSignature()));
        console.sendMessage(TextComponent.fromLegacyText("\n§aValue Decoded: §e" + Arrays.toString(decoded)));
    }

    public enum PlayerOrSkin {
        player,
        skin,
    }
}
