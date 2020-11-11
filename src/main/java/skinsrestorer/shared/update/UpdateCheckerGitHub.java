package skinsrestorer.shared.update;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.inventivetalent.update.spiget.UpdateCallback;
import skinsrestorer.bukkit.SkinsRestorer;
import skinsrestorer.shared.storage.Config;
import skinsrestorer.shared.utils.SRLogger;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by McLive on 11.04.2019.
 */
public class UpdateCheckerGitHub extends UpdateChecker {
    private SRLogger log;
    private String userAgent;
    private String currentVersion;
    private GitHubReleaseInfo releaseInfo;
    private final String resourceId = "SkinsRestorerX";

    private static String Latest_RELEASES_URL = "https://api.github.com/repos/SkinsRestorer/%s/releases/latest";
    private static String RELEASES_URL = "https://api.github.com/repos/SkinsRestorer/SkinsRestorerX/releases";


    public UpdateCheckerGitHub(int resourceId, String currentVersion, SRLogger log, String userAgent) {
        super(resourceId, currentVersion, log, userAgent);
        this.log = log;
        this.userAgent = userAgent;
        this.currentVersion = currentVersion;
    }

    @Override
    public void checkForUpdate(final UpdateCallback callback) {
        String currentVersion = this.currentVersion;
        int currentVersionAge = 0; //
        boolean isDevBuild;
        boolean DelayUpdate = Config.DELAYUPDATE;
        int DelayedVersions = Config.DELAYEDVERSIONS;

        try {
            try {
                currentVersion = currentVersion.substring(0, currentVersion.indexOf("-"));
            } catch (Exception ignored) {
            }
            //Old refresher
            HttpURLConnection connection2 = (HttpURLConnection) new URL(String.format(Latest_RELEASES_URL, this.resourceId)).openConnection();
            connection2.setRequestProperty("User-Agent", this.userAgent);

            JsonObject apiResponse = new JsonParser().parse(new InputStreamReader(connection2.getInputStream())).getAsJsonObject();
            this.releaseInfo = new Gson().fromJson(apiResponse, GitHubReleaseInfo.class);

            //new refresher
            //Get output and put in a array, also making array for LatestAsset to get url & size
            HttpURLConnection connection = (HttpURLConnection) (new URL(String.format(RELEASES_URL, System.currentTimeMillis()))).openConnection();
            connection.setRequestProperty("User-Agent", this.userAgent);
            int responsecode = connection.getResponseCode(); // testing

            if (responsecode !=200) {
                // todo: connect to spiget updater
                log.logAlways(Level.WARNING, "Could not update!");
                return;
            }

            JsonArray output = (new JsonParser()).parse(new InputStreamReader(connection.getInputStream())).getAsJsonArray();
            JsonArray LatestAssetsArray = output.get(0).getAsJsonObject().get("assets").getAsJsonArray();

            int VersionsCount = output.size();
            String LatestVersion = output.get(0).getAsJsonObject().get("tag_name").toString();
            String LatestDownload_url = LatestAssetsArray.get(0).getAsJsonObject().get("browser_download_url").toString();
            String LatestDownload_size = LatestAssetsArray.get(0).getAsJsonObject().get("size").toString();

            for (int i = 0; i < VersionsCount; i++) {
                JsonObject jobject = output.get(i).getAsJsonObject();
                String version = jobject.get("tag_name").toString();

                if (i < 6) // testing
                    System.out.println("ver = " + version + "       if = " + version.contains(currentVersion));

                if (version.contains(currentVersion))
                    currentVersionAge = i++;
            }

            if (currentVersion.toLowerCase().contains("snapshot")) {
                currentVersionAge++; //snapshot is not final version
                isDevBuild = true;

            }


            /*
             *  1. if currentVersionAge = 0 -> callback.upToDate();
             *  2. if DelayUpdate && (currentVersionAge > DelayedVersions) -> callback.UpdateDelayed(....);
             *  3. else = update -> callback.updateAvailable(currentVersion, LatestDownload_url, true);
             */
            if (currentVersionAge <= 0) {
                //todo include isDevBuild
                callback.upToDate();
            } else if (DelayUpdate && (currentVersionAge > DelayedVersions)) {
                //code here for delayed update
            } else {
                callback.updateAvailable(currentVersion, LatestDownload_url, true);
            }


            this.log.log("----------------------------");
            this.log.log("LatestVersion= " + LatestVersion);
            this.log.log("Current= " + currentVersion);
            this.log.log("age= " + currentVersionAge);
            this.log.log("url= " +LatestDownload_url);
            this.log.log("Size= " +LatestDownload_size);
        System.out.println(getLatestResourceInfo());
        } catch (Exception e) {
            // this.log.log(Level.WARNING, "Failed to get release info from api.github.com.");
        }
    }

    public GitHubReleaseInfo getLatestResourceInfo() {
        return this.releaseInfo;
    }

    public List<String> getUpdateDelayedMessages(String newVersion, String downloadUrl, String currentVersion, boolean bungeeMode, int VersionAge) {
        List<String> updateDelayedMessages = new LinkedList<String>();
        updateDelayedMessages.add("§e[§2SkinsRestorer§e] §a----------------------------------------------");
        updateDelayedMessages.add("§e[§2SkinsRestorer§e] §a    +===============+");
        updateDelayedMessages.add("§e[§2SkinsRestorer§e] §a    | §2SkinsRestorer§a |");
        if (bungeeMode) {
            updateDelayedMessages.add("§e[§2SkinsRestorer§e] §a    |---------------|");
            updateDelayedMessages.add("§e[§2SkinsRestorer§e] §a    |  §eBungee Mode§a  |");
        } else {
            try {
                Bukkit.getName(); //try if it is running bukkit
                updateDelayedMessages.add("§e[§2SkinsRestorer§e] §a    |---------------|");
                updateDelayedMessages.add("§e[§2SkinsRestorer§e] §a    |  §9§n§lBukkit only§a  |");
            } catch (NoClassDefFoundError ignored) {
            }
        }
        updateDelayedMessages.add("§e[§2SkinsRestorer§e] §a    +===============+");
        updateDelayedMessages.add("§e[§2SkinsRestorer§e] §a----------------------------------------------");
        updateDelayedMessages.add("§e[§2SkinsRestorer§e] §b    Current version: §c" + currentVersion);
        updateDelayedMessages.add("§e[§2SkinsRestorer§e] §2    version §a" +newVersion+ " §2is available!");
        updateDelayedMessages.add("§e[§2SkinsRestorer§e] §2    Your version is §e" +VersionAge+ " §2 versions behind.");
        updateDelayedMessages.add("§e[§2SkinsRestorer§e] §2    Consider updating from &n" + downloadUrl);
        updateDelayedMessages.add("§e[§2SkinsRestorer§e] §a----------------------------------------------");

        return updateDelayedMessages;
    }
}
