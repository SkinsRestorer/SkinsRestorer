package skinsrestorer.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import co.aikar.commands.contexts.OnlinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.utils.ReflectionUtil;
import skinsrestorer.shared.utils.ServiceChecker;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

/**
 * Created by McLive on 24.01.2019.
 */

@CommandAlias("sr|skinsrestorer") @CommandPermission("%sr")
public class SrCommand extends BaseCommand {
    private SkinsRestorer plugin;

    public SrCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
    }


    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload") @CommandPermission("%srReload")
    @Description("%helpSrReload")
    public void onReload(CommandSender sender) {
        Config.load(plugin.getConfigPath(), plugin.getResource("config.yml"));
        Locale.load(plugin.getConfigPath(), plugin);
        sender.sendMessage(Locale.RELOAD);
    }


    @Subcommand("status") @CommandPermission("%srStatus")
    @Description("%helpSrStatus")
    public void onStatus(CommandSender sender) {
        sender.sendMessage("Checking needed services for SR to work properly...");

        Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
            ServiceChecker checker = new ServiceChecker();
            checker.setMojangAPI(plugin.getMojangAPI());
            checker.checkServices();

            ServiceChecker.ServiceCheckResponse response = checker.getResponse();
            List<String> results = response.getResults();

            for (String result : results) {
                sender.sendMessage(result);
            }
            sender.sendMessage("Working UUID API count: " + response.getWorkingUUID());
            sender.sendMessage("Working Profile API count: " + response.getWorkingProfile());
            if (response.getWorkingUUID() >= 1 && response.getWorkingProfile() >= 1)
                sender.sendMessage("The plugin currently is in a working state.");
            else
                sender.sendMessage("Plugin currently can't fetch new skins. You might check out our discord at https://discordapp.com/invite/012gnzKK9EortH0v2?utm_source=Discord%20Widget&utm_medium=Connect");
            sender.sendMessage("Finished checking services.");
        });
    }


    @Subcommand("drop") @CommandPermission("%srDrop")
    @CommandCompletion("@players")
    @Description("%helpSrDrop")
    public void onDrop(CommandSender sender, OnlinePlayer target) {
        String player = target.getPlayer().getName();
        plugin.getSkinStorage().removeSkinData(player);
        sender.sendMessage(Locale.SKIN_DATA_DROPPED.replace("%player", player));
    }


    @Subcommand("props") @CommandPermission("%srProps")
    @CommandCompletion("@players")
    @Description("%helpSrProps")
    public void onProps(CommandSender sender, OnlinePlayer target) {
        try {
            Object ep = ReflectionUtil.invokeMethod(target.getPlayer(), "getHandle");
            Object profile = ReflectionUtil.invokeMethod(ep, "getProfile");
            Object propmap = ReflectionUtil.invokeMethod(profile, "getProperties");

            Collection<?> props = (Collection<?>) ReflectionUtil.invokeMethod(propmap.getClass(), propmap, "get",
                    new Class[]{Object.class}, "textures");

            if (props == null || props.isEmpty()) {
                sender.sendMessage(Locale.NO_SKIN_DATA);
                return;
            }

            for (Object prop : props) {
                String name = (String) ReflectionUtil.invokeMethod(prop, "getName");
                String value = (String) ReflectionUtil.invokeMethod(prop, "getValue");
                String signature = (String) ReflectionUtil.invokeMethod(prop, "getSignature");

                ConsoleCommandSender cons = Bukkit.getConsoleSender();

                cons.sendMessage("\n§aName: §8" + name);
                cons.sendMessage("\n§aValue : §8" + value);
                cons.sendMessage("\n§aSignature : §8" + signature);

                byte[] decoded = Base64.getDecoder().decode(value);
                cons.sendMessage("\n§aValue Decoded: §e" + Arrays.toString(decoded));

                sender.sendMessage("\n§e" + Arrays.toString(decoded));
                sender.sendMessage("§cMore info in console!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(Locale.NO_SKIN_DATA);
            return;
        }
        sender.sendMessage("§cMore info in console!");
    }
}
