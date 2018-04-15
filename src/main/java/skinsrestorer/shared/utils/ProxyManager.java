package skinsrestorer.shared.utils;

import skinsrestorer.shared.storage.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ProxyManager {

    private static List<String> proxies = new ArrayList<>();

    public static List<String> getList() {
        if (proxies.isEmpty()) {
            System.out.print("[SkinsRestorer] Proxy list is empty. Getting one.");
            return updateProxies();
        }
        return proxies;
    }

    private static List<String> updateProxies() {
        // Check if user added custom proxies in config.yml
        // If yes: use them!
        if (Config.CUSTOM_PROXIES_ENABLED) {
            System.out.println("[SkinsRestorer] Loading custom proxies set in config.yml...");
            return loadCustomConfigProxies();
        }

        // load proxies from remote API
        System.out.println("[SkinsRestorer] Loading proxies from remote API...");

        proxies = new ArrayList<>();
        String url = "https://mcapi.me/McAPI.php?apikey=ihAet4antmkBNdYu43&list=text";
        try {
            proxies = readURL(url);
        } catch (Exception e) {
            System.out.print("[SkinsRestorer] We couldn't update the proxy list. This usually indicates a firewall problem. A detailed error is below.");
            e.printStackTrace();
        }
        return proxies;
    }

    private static List<String> loadCustomConfigProxies() {
        if (Config.CUSTOM_PROXIES_LIST == null)
            return null;

        if (Config.CUSTOM_PROXIES_LIST.isEmpty())
            return null;

        proxies.addAll(Config.CUSTOM_PROXIES_LIST);

        return proxies;
    }

    private static List<String> readURL(String url) {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "SkinsRestorer");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setDoOutput(true);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), Charset.forName("UTF-8")));
            String str = "";
            int limit = 30;
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                str = inputLine;
            }
            String[] asd = str.split("<br/>");
            for (String d : asd) {
                if (limit == 0) {
                    break;
                }
                proxies.add(d);
                limit--;
            }
            in.close();
            return proxies;
        } catch (IOException e) {
            System.out.print("[SkinsRestorer] We couldn't update the proxy list. This usually indicates a firewall problem. A detailed error is below.");
            e.printStackTrace();
            return proxies;
        }
    }
}
