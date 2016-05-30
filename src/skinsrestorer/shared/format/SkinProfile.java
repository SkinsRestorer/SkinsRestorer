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

package skinsrestorer.shared.format;

import skinsrestorer.shared.storage.SkinStorage;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException.Reason;
import skinsrestorer.shared.utils.UUIDUtil;

public class SkinProfile implements Cloneable {

	protected long timestamp;
	protected Profile profile;
	protected SkinProperty skin;

	public SkinProfile(Profile profile, SkinProperty skinData, long creationTime) {
		this.profile = profile;
		this.skin = skinData;
		this.timestamp = creationTime;
	}

	public String getName() {
		return profile.getName();
	}

	public void attemptUpdate() throws SkinFetchFailedException {
		if ((System.currentTimeMillis() - timestamp) <= (2 * 60 * 60 * 1000)) {
			return;
		}
		try {
			SkinProfile newskinprofile = SkinFetchUtils.fetchSkinProfile(profile.getName(),
					UUIDUtil.fromDashlessString(profile.getId()));
			timestamp = System.currentTimeMillis();
			profile = newskinprofile.profile;
			skin = newskinprofile.skin;

			SkinStorage.getInstance().setSkinData(newskinprofile);
		} catch (SkinFetchFailedException e) {
			if (e.getReason() == Reason.NO_PREMIUM_PLAYER || e.getReason() == Reason.NO_SKIN_DATA) {
				timestamp = System.currentTimeMillis();
				return;
			}
		}
	}

	public void applySkin(ApplyFunction applyfunction) {
		if (skin != null) {
			applyfunction.applySkin(skin);
		}
	}

	public SkinProfile cloneAsForced() {
		SkinProfile cloned = this.clone();
		return cloned;
	}

	@Override
	public SkinProfile clone() {
		return new SkinProfile(profile.clone(), skin, timestamp);
	}

	private static final long MONTH = 30L * 24L * 60L * 60L * 1000L;

	public boolean shouldSerialize() {
		return profile.getId() != null && skin != null && (System.currentTimeMillis() - timestamp < MONTH);
	}

	public static interface ApplyFunction {

		public void applySkin(SkinProperty property);

	}

	public SkinProperty getSkinProperty() {
		return skin;
	}

}
