package skinsrestorer.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bstats.bungeecord.MetricsLite;
import skinsrestorer.bungee.commands.AdminCommands;
import skinsrestorer.bungee.commands.PlayerCommands;
import skinsrestorer.bungee.listeners.LoginListener;
import skinsrestorer.shared.storage.Locale;
import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.MojangAPI;
import skinsrestorer.shared.utils.MojangAPI.SkinRequestException;
import skinsrestorer.shared.utils.MySQL;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;

public class SkinsRestorer extends Plugin {

    private static SkinsRestorer instance;
    private MySQL mysql;
    private boolean multibungee;
    private boolean outdated;

    public static SkinsRestorer getInstance() {
        return instance;
    }

    public String checkVersion(CommandSender console) {
        try {
            HttpsURLConnection con = (HttpsURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=2124")
                    .openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("GET");
            String version = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
            if (version.length() <= 13)
                return version;
        } catch (Exception ex) {
            ex.printStackTrace();
            console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §cFailed to check for an update on Spigot."));
        }
        return getVersion();
    }

    public MySQL getMySQL() {
        return mysql;
    }

    public String getVersion() {
        return getDescription().getVersion();
    }

    public boolean isMultiBungee() {
        return multibungee;
    }

    public boolean isOutdated() {
        return outdated;
    }

    File configFile = new File(getDataFolder(), "config.yml");

    public void createDefaultConfig() {
        try {
            if (!getDataFolder().exists())
                getDataFolder().mkdir();

            File file = new File(getDataFolder(), "config.yml");

            if (!file.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    Files.copy(in, file.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            final Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {

        @SuppressWarnings("unused")
        MetricsLite metrics = new MetricsLite(this);

        instance = this;
        createDefaultConfig();
        Locale.load();

        if (configuration.getsTRI) {
            SkinStorage.init(mysql = new MySQL(
                    Config.MYSQL_HOST,
                    Config.MYSQL_PORT,
                    Config.MYSQL_DATABASE,
                    Config.MYSQL_USERNAME,
                    Config.MYSQL_PASSWORD)
            );
        }
        else {
            SkinStorage.init(getDataFolder());
        }

        getProxy().getPluginManager().registerListener(this, new LoginListener());
        getProxy().getPluginManager().registerCommand(this, new AdminCommands());
        getProxy().getPluginManager().registerCommand(this, new PlayerCommands());
        getProxy().registerChannel("SkinsRestorer");
        SkinApplier.init();

        multibungee = Config.MULTIBUNGEE_ENABLED || ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null;

        getProxy().getScheduler().runAsync(new Runnable() {

            @Override
            public void run() {

                CommandSender console = getProxy().getConsole();

                if (Config.UPDATER_ENABLED)
                    if (checkVersion(console).equals(getVersion())) {
                        outdated = false;
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a----------------------------------------------"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    +===============+"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    | SkinsRestorer |"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    +===============+"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a----------------------------------------------"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §b    Current version: §a" + getVersion()));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    This is the latest version!"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a----------------------------------------------"));
                    } else {
                        outdated = true;
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a----------------------------------------------"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    +===============+"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    | SkinsRestorer |"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a    +===============+"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a----------------------------------------------"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §b    Current version: §c" + getVersion()));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §e    A new version is available! Download it at:"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §e    https://www.spigotmc.org/resources/skinsrestorer.2124"));
                        console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §a----------------------------------------------"));
                    }

                if (Config.DEFAULT_SKINS_ENABLED)
                    for (String skin : Config.DEFAULT_SKINS)
                        try {
                            SkinStorage.setSkinData(skin, MojangAPI.getSkinProperty(MojangAPI.getUUID(skin)));
                        } catch (SkinRequestException e) {
                            if (SkinStorage.getSkinData(skin) == null)
                                console.sendMessage(new TextComponent("§e[§2SkinsRestorer§e] §cDefault Skin '" + skin + "' request error:" + e.getReason()));
                        }
            }
        });
    }
}
