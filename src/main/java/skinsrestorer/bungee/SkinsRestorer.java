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
import skinsrestorer.bungee.storage.Locale;
import skinsrestorer.bungee.storage.SkinStorage;
import skinsrestorer.bungee.utils.MojangAPI;
import skinsrestorer.bungee.utils.MojangAPI.SkinRequestException;
import skinsrestorer.bungee.utils.MySQL;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;

public class SkinsRestorer extends Plugin {

    public static YamlConfiguration LANG;
    public static File LANG_FILE;
    private static SkinsRestorer instance;
    File configFile = new File(getDataFolder(), "config.yml");
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

            final Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadLang() {
        File lang = new File(getDataFolder(), "lang.yml");
        if (!lang.exists()) {
            try {
                getDataFolder().mkdir();
                lang.createNewFile();
                InputStream defConfigStream = this.getResource("messages.yml");
                if (defConfigStream != null) {
                    YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
                    defConfig.save(lang);
                    Locale.setFile(defConfig);
                    return defConfig;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        YamlConfiguration conf = YamlConfiguration.loadConfiguration(lang);
        for (Locale item : Locale.values()) {
            if (conf.getString(item.getPath()) == null) {
                conf.set(item.getPath(), item.getDefault());
            }
        }
        Locale.setFile(conf);
        SkinsRestorer.LANG = conf;
        SkinsRestorer.LANG_FILE = lang;
        try {
            conf.save(getLangFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {

        @SuppressWarnings("unused")
        MetricsLite metrics = new MetricsLite(this);

        instance = this;
        createDefaultConfig();
        Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        loadLang();

        if (config.getBoolean("MySQL.Enabled") == true) {
            SkinStorage.init(mysql = new MySQL(
                    config.getString("MySQL.Host"),
                    config.getString("MySQL.Port"),
                    config.getString("MySQL.Database"),
                    config.getString("MySQL.Username"),
                    config.getString("MySQL.Password")
                    )
            );
        } else {
            SkinStorage.init(getDataFolder());
        }

        getProxy().getPluginManager().registerListener(this, new LoginListener());
        getProxy().getPluginManager().registerCommand(this, new AdminCommands());
        getProxy().getPluginManager().registerCommand(this, new PlayerCommands());
        getProxy().registerChannel("SkinsRestorer");
        SkinApplier.init();

        multibungee = config.getBoolean("MultiBungee.Enabled") == true || ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null;

        getProxy().getScheduler().runAsync(new Runnable() {

            @Override
            public void run() {

                CommandSender console = getProxy().getConsole();

                if (config.getBoolean("Updater.Enabled") == true)
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

                if (config.getBoolean("DefaultSkins.Enabled") == true)
                    for (String skin : config.getStringList("DefaultSkins.Names"))
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
