/*
 * SkinsRestorer
 * Copyright (C) 2026  SkinsRestorer Team
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
package net.skinsrestorer.shared.skinsafety;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record SkinSafetyBlocklistSnapshot(
        Set<String> blockedNames,
        Set<String> blockedTextureHashes,
        Set<UUID> blockedPlayerUuids,
        Set<String> blockedPlayerNames,
        Set<String> blockedUrlPrefixes,
        Set<String> blockedDomains,
        Map<SkinSafetyDigestAlgorithm, Set<String>> blockedDigests,
        Set<String> allowTextureHashes,
        Set<UUID> allowPlayerUuids
) {
    public SkinSafetyBlocklistSnapshot {
        blockedNames = Set.copyOf(blockedNames);
        blockedTextureHashes = Set.copyOf(blockedTextureHashes);
        blockedPlayerUuids = Set.copyOf(blockedPlayerUuids);
        blockedPlayerNames = Set.copyOf(blockedPlayerNames);
        blockedUrlPrefixes = Set.copyOf(blockedUrlPrefixes);
        blockedDomains = Set.copyOf(blockedDomains);
        EnumMap<SkinSafetyDigestAlgorithm, Set<String>> normalizedBlockedDigests = new EnumMap<>(SkinSafetyDigestAlgorithm.class);
        blockedDigests.forEach((algorithm, values) -> normalizedBlockedDigests.put(algorithm, Set.copyOf(values)));
        blockedDigests = Map.copyOf(normalizedBlockedDigests);
        allowTextureHashes = Set.copyOf(allowTextureHashes);
        allowPlayerUuids = Set.copyOf(allowPlayerUuids);
    }

    public static SkinSafetyBlocklistSnapshot empty() {
        return new SkinSafetyBlocklistSnapshot(
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Map.of(),
                Set.of(),
                Set.of()
        );
    }

    public Set<String> blockedDigests(SkinSafetyDigestAlgorithm algorithm) {
        return blockedDigests.getOrDefault(algorithm, Set.of());
    }

    public boolean hasDigestRules() {
        return blockedDigests.values().stream().anyMatch(values -> !values.isEmpty());
    }
}
