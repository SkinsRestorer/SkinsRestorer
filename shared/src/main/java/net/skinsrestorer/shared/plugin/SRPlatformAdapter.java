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
package net.skinsrestorer.shared.plugin;

import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.gui.SRInventory;
import net.skinsrestorer.shared.info.Platform;
import net.skinsrestorer.shared.info.PluginInfo;
import net.skinsrestorer.shared.subjects.SRCommandSender;
import net.skinsrestorer.shared.subjects.SRPlayer;
import org.incendo.cloud.CommandManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public interface SRPlatformAdapter {
    CommandManager<SRCommandSender> createCommandManager();

    Collection<SRPlayer> getOnlinePlayers(SRCommandSender sender);

    Optional<SRPlayer> getPlayer(SRCommandSender sender, UUID uniqueId);

    InputStream getResource(String resource);

    default String getResouceAsString(String resource) {
        try (InputStream is = getResource(resource)) {
            if (is == null) {
                throw new IllegalStateException("Could not find resource %s in resources!".formatted(resource));
            }

            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void runAsync(Runnable runnable);

    void runRepeatAsync(Runnable runnable, int delay, int interval, TimeUnit timeUnit);

    String getPlatformVersion();

    String getPlatformName();

    String getPlatformVendor();

    Platform getPlatform();

    List<PluginInfo> getPlugins();

    default Optional<PluginInfo> getPluginInfo(String name) {
        return getPlugins().stream()
                .filter(pluginInfo -> pluginInfo.name().equalsIgnoreCase(name))
                .findFirst();
    }

    Optional<SkinProperty> getSkinProperty(SRPlayer player);

    Object createMetricsInstance();

    /**
     * Force an object to stay alive as long as another plugin is loaded.
     * This can be done by registering a listener to the plugin and keeping a reference to the object in the listener.
     *
     * @param plugin The plugin to keep the object alive.
     * @param object The object to keep alive.
     */
    void extendLifeTime(Object plugin, Object object);

    boolean supportsDefaultPermissions();

    /**
     * Called when the plugin is getting disabled.
     * May be optionally implemented to be called by the bootstrap.
     * Use this to clean up any resources such as schedulers.
     */
    default void shutdownCleanup() {
    }

    void openGUI(SRPlayer player, SRInventory srInventory);
}
