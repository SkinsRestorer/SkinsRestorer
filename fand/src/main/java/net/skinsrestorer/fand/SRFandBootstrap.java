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
package net.skinsrestorer.fand;

import io.fand.api.Fand;
import io.fand.api.plugin.Plugin;
import io.fand.api.plugin.PluginContext;
import net.skinsrestorer.api.semver.SemanticVersion;
import net.skinsrestorer.shared.plugin.SRBootstrapper;
import net.skinsrestorer.shared.plugin.SRServerPlugin;

import java.util.List;

public final class SRFandBootstrap implements Plugin {
    private static final SemanticVersion MINIMUM_FAND_VERSION = new SemanticVersion(0, 8, 2);

    private Runnable shutdownHook = () -> {
    };

    @Override
    public void onEnable(PluginContext context) {
        requireSupportedFandVersion();
        SRBootstrapper.startPlugin(
                hook -> shutdownHook = hook,
                List.of(new SRBootstrapper.PlatformClass<>(PluginContext.class, context)),
                new SRFandLogger(context.logger()),
                false,
                SRFandAdapter.class,
                SRServerPlugin.class,
                context.dataDirectory(),
                SRFandInit.class
        );
    }

    private static void requireSupportedFandVersion() {
        String version = Fand.server().version();
        final SemanticVersion current;
        try {
            current = SemanticVersion.fromString(version);
        } catch (RuntimeException invalidVersion) {
            throw new IllegalStateException("Unable to verify Fand version: " + version, invalidVersion);
        }
        if (current.isOlderThan(MINIMUM_FAND_VERSION)) {
            throw new IllegalStateException(
                    "SkinsRestorer requires Fand 0.8.2 or newer; running " + version);
        }
    }

    @Override
    public void onDisable(PluginContext context) {
        shutdownHook.run();
        shutdownHook = () -> {
        };
    }
}
