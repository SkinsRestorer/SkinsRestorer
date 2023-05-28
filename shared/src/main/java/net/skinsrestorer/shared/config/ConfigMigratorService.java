/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
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
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
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
        return migrateV14Layout(reader, configurationData);
    }

    private boolean migrateV14Layout(PropertyReader reader,
                                     ConfigurationData configData) {
        boolean migrated = false;
        Property<Boolean> oldDebugProperty = newProperty("Debug", false);
        migrated |= moveProperty(oldDebugProperty, DevConfig.DEBUG, reader, configData);

        Property<Boolean> oldMySQLProperty = newProperty("MySQL.Enabled", false);
        migrated |= moveProperty(oldMySQLProperty, DatabaseConfig.MYSQL_ENABLED, reader, configData);

        Property<String> oldMySQLHostProperty = newProperty("MySQL.Host", "localhost");
        migrated |= moveProperty(oldMySQLHostProperty, DatabaseConfig.MYSQL_HOST, reader, configData);

        Property<Integer> oldMySQLPortProperty = newProperty("MySQL.Port", 3306);
        migrated |= moveProperty(oldMySQLPortProperty, DatabaseConfig.MYSQL_PORT, reader, configData);

        Property<String> oldMySQLDatabaseProperty = newProperty("MySQL.Database", "db");
        migrated |= moveProperty(oldMySQLDatabaseProperty, DatabaseConfig.MYSQL_DATABASE, reader, configData);

        Property<String> oldMySQLUsernameProperty = newProperty("MySQL.Username", "root");
        migrated |= moveProperty(oldMySQLUsernameProperty, DatabaseConfig.MYSQL_USERNAME, reader, configData);

        Property<String> oldMySQLPasswordProperty = newProperty("MySQL.Password", "password");
        migrated |= moveProperty(oldMySQLPasswordProperty, DatabaseConfig.MYSQL_PASSWORD, reader, configData);

        Property<Boolean> oldSkinWithoutPermProperty = newProperty("SkinWithoutPerm", true);
        migrated |= moveProperty(oldSkinWithoutPermProperty, CommandConfig.FORCE_DEFAULT_PERMISSIONS, reader, configData);

        Property<Integer> oldSkinChangeCooldownProperty = newProperty("SkinChangeCooldown", 30);
        migrated |= moveProperty(oldSkinChangeCooldownProperty, CommandConfig.SKIN_CHANGE_COOLDOWN, reader, configData);

        Property<Integer> oldSkinErrorCooldownProperty = newProperty("SkinErrorCooldown", 5);
        migrated |= moveProperty(oldSkinErrorCooldownProperty, CommandConfig.SKIN_ERROR_COOLDOWN, reader, configData);

        Property<Boolean> oldEnableCustomHelpProperty = newProperty("EnableCustomHelp", false);
        migrated |= moveProperty(oldEnableCustomHelpProperty, CommandConfig.ENABLE_CUSTOM_HELP, reader, configData);

        Property<Boolean> oldDisablePrefixProperty = newProperty("DisablePrefix", false);
        migrated |= moveProperty(oldDisablePrefixProperty, MessageConfig.DISABLE_PREFIX, reader, configData);

        Property<Boolean> oldDefaultSkinsEnabledProperty = newProperty("DefaultSkins.Enabled", false);
        migrated |= moveProperty(oldDefaultSkinsEnabledProperty, StorageConfig.DEFAULT_SKINS_ENABLED, reader, configData);

        Property<Boolean> oldDefaultSkinsApplyForPremiumProperty = newProperty("DefaultSkins.ApplyForPremium", false);
        migrated |= moveProperty(oldDefaultSkinsApplyForPremiumProperty, StorageConfig.DEFAULT_SKINS_PREMIUM, reader, configData);

        Property<List<String>> oldDefaultSkinsNamesProperty = newListProperty("DefaultSkins.Names", Collections.emptyList()); //todo: include examples xknat & pistonmaster
        migrated |= moveProperty(oldDefaultSkinsNamesProperty, StorageConfig.DEFAULT_SKINS, reader, configData);

        Property<Boolean> oldDisabledSkinsEnabledProperty = newProperty("DisabledSkins.Enabled", false);
        migrated |= moveProperty(oldDisabledSkinsEnabledProperty, CommandConfig.DISABLED_SKINS_ENABLED, reader, configData);

        Property<List<String>> oldDisabledSkinsNamesProperty = newListProperty("DisabledSkins.Names", Collections.emptyList()); //todo: include examples steve & owner
        migrated |= moveProperty(oldDisabledSkinsNamesProperty, CommandConfig.DISABLED_SKINS, reader, configData);

        Property<Boolean> oldNotAllowedCommandServersProperty = newProperty("NotAllowedCommandServers.Enabled", true); // @xknat do we want this true on default???
        migrated |= moveProperty(oldNotAllowedCommandServersProperty, ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS_ENABLED, reader, configData);

        Property<Boolean> oldNotAllowedCommandServersAllowListProperty = newProperty("NotAllowedCommandServers.AllowList", false);
        migrated |= moveProperty(oldNotAllowedCommandServersAllowListProperty, ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS_ALLOWLIST, reader, configData);

        Property<Boolean> oldNotAllowedCommandServersIfNoServerBlockCommandProperty = newProperty("NotAllowedCommandServers.IfNoServerBlockCommand", false);
        migrated |= moveProperty(oldNotAllowedCommandServersIfNoServerBlockCommandProperty, ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS_IF_NONE_BLOCK_COMMAND, reader, configData);

        Property<List<String>> oldNotAllowedCommandServersListProperty = newListProperty("NotAllowedCommandServers.List", Collections.emptyList()); //todo: include "auth"
        migrated |= moveProperty(oldNotAllowedCommandServersListProperty, ProxyConfig.NOT_ALLOWED_COMMAND_SERVERS, reader, configData);

        Property<Boolean> oldCustomGUIEnabledProperty = newProperty("CustomGUI.Enabled", false);
        migrated |= moveProperty(oldCustomGUIEnabledProperty, GUIConfig.CUSTOM_GUI_ENABLED, reader, configData);

        Property<Boolean> oldCustomGUIShowOnlyCustomGUIProperty = newProperty("CustomGUI.ShowOnlyCustomGUI", true);
        migrated |= moveProperty(oldCustomGUIShowOnlyCustomGUIProperty, GUIConfig.CUSTOM_GUI_ONLY, reader, configData);

        Property<List<String>> oldCustomGUINamesProperty = newListProperty("CustomGUI.Names", Collections.emptyList()); //todo: include examples xknat & pistonmaster
        migrated |= moveProperty(oldCustomGUINamesProperty, GUIConfig.CUSTOM_GUI_SKINS, reader, configData);

        Property<Boolean> oldPerSkinPermissionsProperty = newProperty("PerSkinPermissions", false);
        migrated |= moveProperty(oldPerSkinPermissionsProperty, CommandConfig.PER_SKIN_PERMISSIONS, reader, configData);

        Property<Integer> oldSkinExpiresAfterProperty = newProperty("SkinExpiresAfter", 15);
        migrated |= moveProperty(oldSkinExpiresAfterProperty, StorageConfig.SKIN_EXPIRES_AFTER, reader, configData);

        Property<Boolean> oldNoSkinIfLoginCanceledProperty = newProperty("NoSkinIfLoginCanceled", true);
        migrated |= moveProperty(oldNoSkinIfLoginCanceledProperty, LoginConfig.NO_SKIN_IF_LOGIN_CANCELED, reader, configData);

        Property<Boolean> oldAlwaysApplyPremiumProperty = newProperty("AlwaysApplyPremium", false);
        migrated |= moveProperty(oldAlwaysApplyPremiumProperty, LoginConfig.ALWAYS_APPLY_PREMIUM, reader, configData);

        Property<Boolean> oldRestrictSkinUrlsEnabledProperty = newProperty("RestrictSkinUrls.Enabled", false);
        migrated |= moveProperty(oldRestrictSkinUrlsEnabledProperty, CommandConfig.RESTRICT_SKIN_URLS_ENABLED, reader, configData);

        Property<List<String>> oldRestrictSkinUrlsListProperty = newListProperty("RestrictSkinUrls.List", Collections.emptyList()); //todo: include examples
        migrated |= moveProperty(oldRestrictSkinUrlsListProperty, CommandConfig.RESTRICT_SKIN_URLS_LIST, reader, configData);

        Property<String> oldMineskinAPIKeyProperty = newProperty("MineskinAPIKey", "key");
        migrated |= moveProperty(oldMineskinAPIKeyProperty, APIConfig.MINESKIN_API_KEY, reader, configData);

        Property<Boolean> oldResourcePackFixProperty = newProperty("ResourcePackFix", true);
        migrated |= moveProperty(oldResourcePackFixProperty, ServerConfig.RESOURCE_PACK_FIX, reader, configData);

        Property<Boolean> oldDismountPlayerOnSkinUpdateProperty = newProperty("DismountPlayerOnSkinUpdate", true);
        migrated |= moveProperty(oldDismountPlayerOnSkinUpdateProperty, ServerConfig.DISMOUNT_PLAYER_ON_UPDATE, reader, configData);

        Property<Boolean> oldRemountPlayerOnSkinUpdateProperty = newProperty("RemountPlayerOnSkinUpdate", true);
        migrated |= moveProperty(oldRemountPlayerOnSkinUpdateProperty, ServerConfig.REMOUNT_PLAYER_ON_UPDATE, reader, configData);

        Property<Boolean> oldDismountPassengersOnSkinUpdateProperty = newProperty("DismountPassengersOnSkinUpdate", false);
        migrated |= moveProperty(oldDismountPassengersOnSkinUpdateProperty, ServerConfig.DISMOUNT_PASSENGERS_ON_UPDATE, reader, configData);

        Property<Boolean> oldDisableOnJoinSkinsProperty = newProperty("DisableOnJoinSkins", false);
        migrated |= moveProperty(oldDisableOnJoinSkinsProperty, AdvancedConfig.DISABLE_ON_JOIN_SKINS, reader, configData);

        Property<Boolean> oldDisallowAutoUpdateSkinProperty = newProperty("DisallowAutoUpdateSkin", false);
        migrated |= moveProperty(oldDisallowAutoUpdateSkinProperty, StorageConfig.DISALLOW_AUTO_UPDATE_SKIN, reader, configData);

        Property<Boolean> oldEnablePaperJoinListenerProperty = newProperty("EnablePaperJoinListener", true);
        migrated |= moveProperty(oldEnablePaperJoinListenerProperty, AdvancedConfig.ENABLE_PAPER_JOIN_LISTENER, reader, configData);

        Property<Boolean> oldForwardTexturesProperty = newProperty("ForwardTextures", true);
        migrated |= moveProperty(oldForwardTexturesProperty, AdvancedConfig.FORWARD_TEXTURES, reader, configData);

        if (Boolean.TRUE.equals(configData.getValue(DatabaseConfig.MYSQL_ENABLED))) {
            Property<String> oldMySQLSkinTable = newProperty("MySQL.SkinTable", "Skins");
            if (oldMySQLSkinTable.isValidInResource(reader)) {
                try {
                    Files.write(plugin.getDataFolder().resolve("legacy_skin_table.txt"),
                            oldMySQLSkinTable.determineValue(reader).getValue().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Property<String> oldMySQLPlayerTable = newProperty("MySQL.PlayerTable", "Players");
            if (oldMySQLPlayerTable.isValidInResource(reader)) {
                try {
                    Files.write(plugin.getDataFolder().resolve("legacy_player_table.txt"),
                            oldMySQLPlayerTable.determineValue(reader).getValue().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return migrated;
    }

    protected <T> boolean moveProperty(Property<T> oldProperty,
                                       Property<T> newProperty,
                                       PropertyReader reader,
                                       ConfigurationData configData) {
        PropertyValue<T> oldPropertyValue = oldProperty.determineValue(reader);
        if (oldPropertyValue.isValidInResource()) {
            if (reader.contains(newProperty.getPath())) {
                logger.info("Detected deprecated property " + oldProperty.getPath());
            } else {
                logger.info("Renaming " + oldProperty.getPath() + " to " + newProperty.getPath());
                configData.setValue(newProperty, oldPropertyValue.getValue());
            }
            return true;
        }
        return false;
    }
}
