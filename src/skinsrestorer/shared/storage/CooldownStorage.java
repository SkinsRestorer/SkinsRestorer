/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */


package skinsrestorer.shared.storage;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CooldownStorage {

	private CooldownStorage() {
	}

	private static final CooldownStorage instance = new CooldownStorage();

	public static CooldownStorage getInstance() {
		return instance;
	}

	protected final ConcurrentHashMap<UUID, Long> cooldown = new ConcurrentHashMap<UUID, Long>();

	public void setCooldown(UUID playeruuid, int cooldowntime, TimeUnit timeunit) {
		cooldown.put(playeruuid, timeunit.toMillis(cooldowntime) + System.currentTimeMillis());
	}

	public boolean isAtCooldown(UUID playeruuid) {
		Long expire = cooldown.get(playeruuid);
		if (expire != null) {
			return expire > System.currentTimeMillis();
		}
		return false;
	}

	public void resetCooldown(UUID playeruuid) {
		cooldown.remove(playeruuid);
	}

	public static final Runnable cleanupCooldowns = new Runnable() {
		@Override
		public void run() {
			long current = System.currentTimeMillis();
			Iterator<Long> iterator = CooldownStorage.getInstance().cooldown.values().iterator();
			while (iterator.hasNext()) {
				if (iterator.next() <= current) {
					iterator.remove();
				}
			}
		}
	};

}
