package skinsrestorer.shared.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ProxyManager {

    public static List<String> proxies = new ArrayList<String>();
    private static String inputLine;

    public void loadProxies() {
        String line;
        try (
                InputStream is = getClass().getResourceAsStream("proxy.txt");
                InputStreamReader isr = new InputStreamReader(is, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
        ) {
            while ((line = br.readLine()) != null) {
                proxies.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getList() {
        if (proxies.isEmpty()) {
            System.out.print("[SkinsRestorer] Proxy list is empty. Getting one.");
            return updateProxies();
        }
        return proxies;
    }

    public static List<String> updateProxies() {
        proxies = new ArrayList<String>();
        String url = "https://mcapi.me/McAPI.php?apikey=ihAet4antmkBNdYu43&list=text";
        try {
            List<String> pp = readURL(url);
            proxies = pp;
            System.out.println(proxies);
        } catch (IOException e) {
            System.out.print("[SkinsRestorer] We couldn't update the proxy list. This usually indicates a firewall problem. A detailed error is below.");
            e.printStackTrace();
        }
        return proxies;
    }

    private static List<String> readURL(String url) throws IOException {
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
