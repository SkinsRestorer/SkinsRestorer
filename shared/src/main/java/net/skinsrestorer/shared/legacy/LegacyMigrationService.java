package net.skinsrestorer.shared.legacy;

import ch.jalu.configme.SettingsManager;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;

import javax.inject.Inject;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class LegacyMigrationService {
    private final SettingsManager settings;
    private final SRPlugin plugin;
    private final SRLogger logger;

    public void migrate() {

    }
}
