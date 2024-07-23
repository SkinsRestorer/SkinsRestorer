/*
 * SkinsRestorer
 * Copyright (C) 2024  SkinsRestorer Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.skinsrestorer.shared.storage.adapter.file.model.cooldown;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.skinsrestorer.shared.storage.adapter.StorageAdapter;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class CooldownFile {
    private static final int CURRENT_DATA_VERSION = 1;
    private UUID uniqueId;
    private String groupName;
    private long creationTime;
    private long duration;
    private int dataVersion;

    public static CooldownFile fromCooldownData(StorageAdapter.StorageCooldown cooldownData) {
        CooldownFile cooldownFile = new CooldownFile();
        cooldownFile.uniqueId = cooldownData.owner();
        cooldownFile.groupName = cooldownData.groupName();
        cooldownFile.creationTime = cooldownData.creationTime().getEpochSecond();
        cooldownFile.duration = cooldownData.duration().getSeconds();
        cooldownFile.dataVersion = CURRENT_DATA_VERSION;
        return cooldownFile;
    }

    public StorageAdapter.StorageCooldown toCooldownData() {
        return new StorageAdapter.StorageCooldown(
                uniqueId,
                groupName,
                Instant.ofEpochSecond(creationTime),
                Duration.ofSeconds(duration)
        );
    }
}
