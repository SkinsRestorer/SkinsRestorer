package skinsrestorer.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.MetricsLite;
import org.inventivetalent.update.spiget.UpdateCallback;
import skinsrestorer.bungee.commands.AdminCommands;
import skinsrestorer.bungee.commands.PlayerCommands;
import skinsrestorer.bungee.listeners.LoginListener;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.MySQL;
import skinsrestorer.shared.utils.UpdateChecker;

public class SkinsRestorer extends Plugin {

    private static SkinsRestorer instance;
    private MySQL mysql;
    private boolean multibungee;
    private boolean outdated;
    private UpdateChecker updateChecker;
    private CommandSender console;

    public static SkinsRestorer getInstance() {
        return instance;
    }

    public MySQL getMySQL() {
        return mysql;
    }

    public String getVersion() {
        return getDescription().getVersion().replace("-SNAPSHOT", "");
    }

    public boolean isMultiBungee() {
        return multibungee;
    }

    public boolean isOutdated() {
        return outdated;
    }

    @Override
    public void onEnable() {
        @SuppressWarnings("unused")
        MetricsLite metrics = new MetricsLite(this);

        console = getProxy().getConsole();

        updateChecker = new UpdateChecker(2124, this.getDescription().getVersion(), this.getLogger(), "SkinsRestorerUpdater/BungeeCord");
        this.checkUpdate();

        instance = this;
        Config.load(getResourceAsStream("config.yml"));
        Locale.load();

        if (Config.USE_MYSQL) {
            SkinStorage.init(mysql = new MySQL(
                    Config.MYSQL_HOST,
                    Config.MYSQL_PORT,
                    Config.MYSQL_DATABASE,
                    Config.MYSQL_USERNAME,
                    Config.MYSQL_PASSWORD
            ));
        } else {
            SkinStorage.init(getDataFolder());
        }

        getProxy().getPluginManager().registerListener(this, new LoginListener(this));
        getProxy().getPluginManager().registerCommand(this, new AdminCommands());
        getProxy().getPluginManager().registerCommand(this, new PlayerCommands());
        getProxy().registerChannel("sr:skinchange");
        SkinApplier.init();

        multibungee = Config.MULTIBUNGEE_ENABLED || ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null;

        ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), () -> {
            if (Config.DEFAULT_SKINS_ENABLED)
                for (String skin : Config.DEFAULT_SKINS)
                    try {
                        SkinStorage.setSkinData(skin, MojangAPI.getSkinProperty(MojangAPI.getUUID(skin)));
                    } catch (SkinRequestException e) {
                        if (SkinStorage.getSkinData(skin) == null)
                            console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §cDefault Skin '" + skin + "' request error:" + e.getReason()));
                    }
        });
    }

    private void checkUpdate() {
        ProxyServer.getInstance().getScheduler().runAsync(this, () -> {
            if (Config.UPDATER_ENABLED) {
                updateChecker.checkForUpdate(new UpdateCallback() {
                    @Override
                    public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a----------------------------------------------"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    +===============+"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    | SkinsRestorer |"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    +===============+"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a----------------------------------------------"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §b    Current version: §c" + getVersion()));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §e    A new version is available! Download it at:"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §e    " + downloadUrl));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a----------------------------------------------"));
                    }

                    @Override
                    public void upToDate() {
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a----------------------------------------------"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    +===============+"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    | SkinsRestorer |"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    +===============+"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a----------------------------------------------"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §b    Current version: §a" + getVersion()));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    This is the latest version!"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a----------------------------------------------"));
                    }
                });
            }
        });
    }
}
