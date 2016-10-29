package skinsrestorer.shared.storage;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CooldownStorage implements Runnable {
	private static final ConcurrentHashMap<String, Long> cooldown = new ConcurrentHashMap<>();

	public static void setCooldown(String name, int cooldowntime, TimeUnit timeunit) {
		cooldown.put(name, timeunit.toMillis(cooldowntime) + System.currentTimeMillis());
	}

	public static boolean hasCooldown(String name) {
		Long expire = cooldown.get(name);
		if (expire != null) {
			return expire > System.currentTimeMillis();
		}
		return false;
	}

	public static void resetCooldown(String name) {
		cooldown.remove(name);
	}

	@Override
	public void run() {
		long current = System.currentTimeMillis();
		Iterator<Long> iterator = CooldownStorage.cooldown.values().iterator();
		while (iterator.hasNext()) {
			if (iterator.next() <= current) {
				iterator.remove();
			}
		}
	}

}