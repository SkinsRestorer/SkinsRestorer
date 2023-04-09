package net.skinsrestorer.shared.config;

import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.migration.MigrationService;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.properties.convertresult.PropertyValue;
import ch.jalu.configme.resource.PropertyReader;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.log.SRLogger;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConfigMigratorService implements MigrationService {
    private final SRLogger logger;

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
