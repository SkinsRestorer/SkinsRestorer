package skinsrestorer.shared.storage;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CooldownStorage implements Runnable {
    private static final ConcurrentHashMap<String, Long> cooldown = new ConcurrentHashMap<>();

    public static boolean hasCooldown(String name) {
        Long expire = cooldown.get(name);
        if (expire != null)
            return expire > System.currentTimeMillis();
        return false;
    }

    public static void resetCooldown(String name) {
        cooldown.remove(name);
    }

    public static int getCooldown(String name) {
        int int1 = Integer.valueOf(String.format("%d",
                TimeUnit.MILLISECONDS.toSeconds(cooldown.get(name))));
        int int2 = Integer.valueOf(String.format("%d",
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        return int1 - int2;

    }

    public static void setCooldown(String name, int cooldowntime, TimeUnit timeunit) {
        cooldown.put(name, System.currentTimeMillis() + timeunit.toMillis(cooldowntime));
    }

    @Override
    public void run() {
        long current = System.currentTimeMillis();
        Iterator<Long> iterator = CooldownStorage.cooldown.values().iterator();
        while (iterator.hasNext())
            if (iterator.next() <= current) {
                iterator.remove();
            }
    }

}