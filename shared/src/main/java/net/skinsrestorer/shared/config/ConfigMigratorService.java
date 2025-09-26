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
package net.skinsrestorer.shared.config;

import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.migration.MigrationService;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.convertresult.PropertyValue;
import ch.jalu.configme.resource.PropertyReader;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.utils.SRHelpers;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConfigMigratorService implements MigrationService {
    private final SRLogger logger;
    private final SRPlugin plugin;

    @Override
    public boolean checkAndMigrate(@NotNull PropertyReader reader, @NotNull ConfigurationData configurationData) {
        if (performMigrations(reader, configurationData) == MIGRATION_REQUIRED
                || !configurationData.areAllValuesValidInResource()) {
            return MIGRATION_REQUIRED;
        }
        return NO_MIGRATION_NEEDED;
    }

    private boolean performMigrations(@NotNull PropertyReader reader, @NotNull ConfigurationData configurationData) {
        // Use | instead of || to ensure all methods are called in the chain
        return migrateV14Layout(reader, configurationData)
                | migrateNewGUILayout(reader, configurationData);
    }

    private boolean migrateV14Layout(PropertyReader reader,
                                     ConfigurationData configData) {
        boolean migrated = false;
        migrated |= moveProperty(newProperty("Debug", false), DevConfig.DEBUG, reader, configData);
        migrated |= moveProperty(newProperty("MySQL.Host", "localhost"), DatabaseConfig.MYSQL_HOST, reader, configData);
        migrated |= moveProperty(newProperty("MySQL.Port", 3306), DatabaseConfig.MYSQL_PORT, reader, configData);
        migrated |= moveProperty(newProperty("MySQL.Database", "db"), DatabaseConfig.MYSQL_DATABASE, reader, configData);
        migrated |= moveProperty(newProperty("MySQL.Username", "root"), DatabaseConfig.MYSQL_USERNAME, reader, configData);
        migrated |= moveProperty(newProperty("MySQL.Password", "password"), DatabaseConfig.MYSQL_PASSWORD, reader, configData);
        migrated |= moveProperty(newProperty("MySQL.ConnectionOptions", ""), DatabaseConfig.MYSQL_CONNECTION_OPTIONS, reader, configData);

        boolean migratedDatabaseType = migrateDatabaseType(reader, configData, newProperty("database.enabled", false));
        if (!migratedDatabaseType) {
            migratedDatabaseType = migrateDatabaseType(reader, configData, newProperty("MySQL.Enabled", false));
        }
        migrated |= migratedDatabaseType;
        migrated |= moveProperty(newProperty("SkinWithoutPerm", true), CommandConfig.FORCE_DEFAULT_PERMISSIONS, reader, configData);
        migrated |= moveProperty(newProperty("SkinChangeCooldown", 30), CommandConfig.SKIN_CHANGE_COOLDOWN, reader, configData);
        migrated |= moveProperty(newProperty("SkinErrorCooldown", 5), CommandConfig.SKIN_ERROR_COOLDOWN, reader, configData);
        migrated |= moveProperty(newProperty("EnableCustomHelp", false), CommandConfig.CUSTOM_HELP_ENABLED, reader, configData);
        migrated |= moveProperty(newProperty("DisablePrefix", false), MessageConfig.DISABLE_PREFIX, reader, configData);
        migrated |= moveProperty(newProperty("DefaultSkins.Enabled", false), StorageConfig.DEFAULT_SKINS_ENABLED, reader, configData);
        migrated |= moveProperty(newProperty("DefaultSkins.ApplyForPremium", false), StorageConfig.DEFAULT_SKINS_PREMIUM, reader, configData);
        migrated |= moveProperty(newListProperty("DefaultSkins.Names", List.of()), StorageConfig.DEFAULT_SKINS, reader, configData);
        migrated |= moveProperty(newProperty("DisabledSkins.Enabled", false), CommandConfig.DISABLED_SKINS_ENABLED, reader, configData);
        migrated |= moveProperty(newListProperty("DisabledSkins.Names", List.of()), CommandConfig.DISABLED_SKINS, reader, configData);
        migrated |= moveProperty(newProperty("NotAllowedCommandServers.Enabled", true), ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS_ENABLED, reader, configData);
        migrated |= moveProperty(newProperty("NotAllowedCommandServers.AllowList", false), ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS_ALLOWLIST, reader, configData);
        migrated |= moveProperty(newProperty("NotAllowedCommandServers.IfNoServerBlockCommand", false), ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS_IF_NONE_BLOCK_COMMAND, reader, configData);
        migrated |= moveProperty(newListProperty("NotAllowedCommandServers.List", List.of()), ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS, reader, configData);
        migrated |= moveProperty(newProperty("CustomGUI.Enabled", false), GUIConfig.CUSTOM_GUI_ENABLED, reader, configData);
        migrated |= moveProperty(newProperty("CustomGUI.ShowOnlyCustomGUI", true), GUIConfig.CUSTOM_GUI_ONLY_LIST, reader, configData);
        migrated |= moveProperty(newListProperty("CustomGUI.Names", List.of()), GUIConfig.CUSTOM_GUI_LIST, reader, configData);
        migrated |= moveProperty(newProperty("PerSkinPermissions", false), CommandConfig.PER_SKIN_PERMISSIONS, reader, configData);
        migrated |= moveProperty(newProperty("SkinExpiresAfter", 15), StorageConfig.SKIN_EXPIRES_AFTER, reader, configData);
        migrated |= moveProperty(newProperty("NoSkinIfLoginCanceled", true), LoginConfig.NO_SKIN_IF_LOGIN_CANCELED, reader, configData);
        migrated |= moveProperty(newProperty("AlwaysApplyPremium", false), LoginConfig.ALWAYS_APPLY_PREMIUM, reader, configData);
        migrated |= moveProperty(newProperty("RestrictSkinUrls.Enabled", false), CommandConfig.RESTRICT_SKIN_URLS_ENABLED, reader, configData);
        migrated |= moveProperty(newListProperty("RestrictSkinUrls.List", List.of()), CommandConfig.RESTRICT_SKIN_URLS_LIST, reader, configData);
        migrated |= moveProperty(newProperty("MineskinAPIKey", "key"), APIConfig.MINESKIN_API_KEY, reader, configData);
        migrated |= moveProperty(newProperty("ResourcePackFix", true), ServerConfig.RESOURCE_PACK_FIX, reader, configData);
        migrated |= moveProperty(newProperty("DismountPlayerOnSkinUpdate", true), ServerConfig.DISMOUNT_PLAYER_ON_UPDATE, reader, configData);
        migrated |= moveProperty(newProperty("RemountPlayerOnSkinUpdate", true), ServerConfig.REMOUNT_PLAYER_ON_UPDATE, reader, configData);
        migrated |= moveProperty(newProperty("DismountPassengersOnSkinUpdate", false), ServerConfig.DISMOUNT_PASSENGERS_ON_UPDATE, reader, configData);
        migrated |= moveProperty(newProperty("DisableOnJoinSkins", false), AdvancedConfig.DISABLE_ON_JOIN_SKINS, reader, configData);
        migrated |= moveProperty(newProperty("DisallowAutoUpdateSkin", false), StorageConfig.DISALLOW_AUTO_UPDATE_SKIN, reader, configData);
        migrated |= moveProperty(newProperty("EnablePaperJoinListener", true), AdvancedConfig.ENABLE_PAPER_JOIN_LISTENER, reader, configData);

        if (configData.getValue(DatabaseConfig.DATABASE_TYPE) == DatabaseConfig.DatabaseType.MYSQL) {
            Property<String> oldMySQLSkinTable = newProperty("MySQL.SkinTable", "Skins");
            if (oldMySQLSkinTable.isValidInResource(reader)) {
                try {
                    SRHelpers.writeIfNeeded(plugin.getDataFolder().resolve("legacy_skin_table.txt"),
                            oldMySQLSkinTable.determineValue(reader).getValue());
                } catch (IOException e) {
                    logger.severe("Failed to write legacy_skin_table.txt", e);
                }
            }

            Property<String> oldMySQLPlayerTable = newProperty("MySQL.PlayerTable", "Players");
            if (oldMySQLPlayerTable.isValidInResource(reader)) {
                try {
                    SRHelpers.writeIfNeeded(plugin.getDataFolder().resolve("legacy_player_table.txt"),
                            oldMySQLPlayerTable.determineValue(reader).getValue());
                } catch (IOException e) {
                    logger.severe("Failed to write legacy_player_table.txt", e);
                }
            }
        }

        return migrated;
    }

    private boolean migrateNewGUILayout(PropertyReader reader,
                                        ConfigurationData configData) {
        boolean migrated = false;
        migrated |= moveProperty(newProperty("customGUI.enabled", true), GUIConfig.CUSTOM_GUI_ENABLED, reader, configData);
        migrated |= moveProperty(newProperty("customGUI.showOnlyCustomGUI", false), GUIConfig.CUSTOM_GUI_ONLY_LIST, reader, configData);
        migrated |= moveProperty(newListProperty("customGUI.list", List.of()), GUIConfig.CUSTOM_GUI_LIST, reader, configData);

        return migrated;
    }

    private boolean migrateDatabaseType(PropertyReader reader,
                                        ConfigurationData configData,
                                        Property<Boolean> oldProperty) {
        PropertyValue<Boolean> oldValue = oldProperty.determineValue(reader);
        if (!oldValue.isValidInResource()) {
            return false;
        }

        boolean enabled = Boolean.TRUE.equals(oldValue.getValue());
        configData.setValue(DatabaseConfig.DATABASE_TYPE, enabled ? DatabaseConfig.DatabaseType.MYSQL : DatabaseConfig.DatabaseType.FILE);
        return true;
    }

    protected <T> boolean moveProperty(Property<T> oldProperty,
                                       Property<T> newProperty,
                                       PropertyReader reader,
                                       ConfigurationData configData) {
        PropertyValue<T> oldPropertyValue = oldProperty.determineValue(reader);
        if (oldPropertyValue.isValidInResource()) {
            if (reader.contains(newProperty.getPath())) {
                logger.info("Detected deprecated property %s".formatted(oldProperty.getPath()));
            } else {
                logger.info("Renaming %s to %s".formatted(oldProperty.getPath(), newProperty.getPath()));
                configData.setValue(newProperty, oldPropertyValue.getValue());
            }
            return true;
        }
        return false;
    }
}
