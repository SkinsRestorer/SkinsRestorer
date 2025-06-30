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
package net.skinsrestorer.bukkit.utils;

import net.skinsrestorer.bukkit.mappings.*;
import net.skinsrestorer.bukkit.paper.PaperUtil;
import org.bukkit.Server;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class MappingManager {
    private static final List<IMapping> MAPPINGS = List.of(
            new Mapping1_18(),
            new Mapping1_18_2(),
            new Mapping1_19(),
            new Mapping1_19_1(),
            new Mapping1_19_2(),
            new Mapping1_19_3(),
            new Mapping1_19_4(),
            new Mapping1_20(),
            new Mapping1_20_2(),
            new Mapping1_20_4(),
            new Mapping1_20_5(),
            new Mapping1_21(),
            new Mapping1_21_2(),
            new Mapping1_21_4(),
            new Mapping1_21_5(),
            new Mapping1_21_6()
    );

    public static Optional<IMapping> getMapping(Server server) {
        Optional<String> spigotMappingVersion = getSpigotMappingVersion(server);
        Optional<String> paperMinecraftVersionId = getPaperMinecraftVersionId();

        if (spigotMappingVersion.isEmpty() && paperMinecraftVersionId.isEmpty()) {
            return Optional.empty();
        }

        Predicate<IMapping> isSupportedSpigotMapping = mapping -> spigotMappingVersion
                .map(mapping.getSpigotMappingVersions()::contains)
                .orElse(false);
        Predicate<IMapping> isSupportedPaperMapping = mapping -> paperMinecraftVersionId
                .map(mapping.getPaperMinecraftVersionIds()::contains)
                .orElse(false);

        return MAPPINGS.stream()
                .filter(mapping -> isSupportedSpigotMapping.test(mapping) || isSupportedPaperMapping.test(mapping))
                .findFirst();
    }

    @SuppressWarnings({"deprecation"})
    public static Optional<String> getSpigotMappingVersion(Server server) {
        var craftMagicNumbers = server.getUnsafe();
        try {
            Method method = craftMagicNumbers.getClass().getMethod("getMappingsVersion");
            return ((String) method.invoke(craftMagicNumbers, new Object[0])).describeConstable();
        } catch (ReflectiveOperationException e) {
            // Happens on paper >= 1.21.6
            return Optional.empty();
        }
    }

    public static Optional<String> getPaperMinecraftVersionId() {
        return PaperUtil.getMinecraftVersionId();
    }
}
