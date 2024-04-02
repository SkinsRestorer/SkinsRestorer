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
package net.skinsrestorer.bukkit;

import ch.jalu.configme.SettingsManager;
import ch.jalu.injector.Injector;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.semver.SemanticVersion;
import net.skinsrestorer.bukkit.command.SRBukkitCommand;
import net.skinsrestorer.bukkit.command.SRHelpTopic;
import net.skinsrestorer.bukkit.hooks.SRPlaceholderAPIExpansion;
import net.skinsrestorer.bukkit.listener.InventoryListener;
import net.skinsrestorer.bukkit.listener.PlayerJoinListener;
import net.skinsrestorer.bukkit.listener.PlayerResourcePackStatusListener;
import net.skinsrestorer.bukkit.listener.ServerMessageListener;
import net.skinsrestorer.bukkit.paper.PaperPlayerJoinEvent;
import net.skinsrestorer.bukkit.refresher.*;
import net.skinsrestorer.bukkit.utils.BukkitPropertyApplier;
import net.skinsrestorer.bukkit.utils.BukkitReflection;
import net.skinsrestorer.bukkit.utils.NoMappingException;
import net.skinsrestorer.bukkit.utils.SkinApplyBukkitAdapter;
import net.skinsrestorer.bukkit.v1_7.BukkitLegacyPropertyApplier;
import net.skinsrestorer.bukkit.wrapper.WrapperBukkit;
import net.skinsrestorer.shared.config.AdvancedConfig;
import net.skinsrestorer.shared.exception.InitializeException;
import net.skinsrestorer.shared.info.ClassInfo;
import net.skinsrestorer.shared.log.SRChatColor;
import net.skinsrestorer.shared.log.SRLogLevel;
import net.skinsrestorer.shared.log.SRLogger;
import net.skinsrestorer.shared.plugin.SRPlugin;
import net.skinsrestorer.shared.plugin.SRServerPlatformInit;
import net.skinsrestorer.shared.subjects.messages.SkinsRestorerLocale;
import net.skinsrestorer.shared.subjects.permissions.PermissionGroup;
import net.skinsrestorer.shared.subjects.permissions.PermissionRegistry;
import net.skinsrestorer.shared.utils.ComponentHelper;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import net.skinsrestorer.shared.utils.SRConstants;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.SimplePluginManager;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SRBukkitInit implements SRServerPlatformInit {
    private final SRPlugin plugin;
    private final SRBukkitAdapter adapter;
    private final Injector injector;
    private final SRLogger logger;
    private final Server server;
    private final WrapperBukkit wrapper;
    private final SkinsRestorerLocale locale;
    private final SettingsManager settingsManager;

    @Override
    public void initSkinApplier() {
        injector.register(SkinApplyBukkitAdapter.class, selectSkinApplyAdapter());

        try {
            injector.register(SkinRefresher.class, detectRefresh());
        } catch (NoMappingException e) {
            logger.severe("Your Minecraft version is not supported by this version of SkinsRestorer! Is there a newer version available? If not, join our discord server!", e);
            throw new IllegalStateException(e);
        } catch (InitializeException e) {
            logger.severe(SRChatColor.RED + SRChatColor.UNDERLINE.toString() + "Could not initialize SkinApplier! Please report this on our discord server!", e);
            throw new IllegalStateException(e);
        }

        plugin.registerSkinApplier(injector.getSingleton(SkinApplierBukkit.class), Player.class, wrapper::player);

        // Log information about the platform
        logger.info(SRChatColor.GREEN + "Running on Minecraft " + SRChatColor.YELLOW + BukkitReflection.SERVER_VERSION + SRChatColor.GREEN + ".");

        if (!BukkitReflection.SERVER_VERSION.isNewerThan(new SemanticVersion(1, 7, 10))) {
            logger.warning(SRChatColor.YELLOW + "Although SkinsRestorer allows using this ancient version, we will not provide full support for it. This version of Minecraft does not allow using all of SkinsRestorers features due to client side restrictions. Please be aware things WILL BREAK and not work!");
        }
    }

    private SkinApplyBukkitAdapter selectSkinApplyAdapter() {
        if (ReflectionUtil.classExists("com.mojang.authlib.GameProfile")) {
            logger.debug("Using BukkitPropertyApplier");
            return injector.getSingleton(BukkitPropertyApplier.class);
        } else {
            logger.debug("Using BukkitLegacyPropertyApplier");
            return injector.getSingleton(BukkitLegacyPropertyApplier.class);
        }
    }

    private SkinRefresher detectRefresh() throws InitializeException, NoMappingException {
        if (SRPlugin.isUnitTest()) {
            return SkinRefresher.NO_OP;
        }

        if (isPaper()) {
            // force SpigotSkinRefresher for unsupported plugins (ViaVersion & other ProtocolHack).
            // Ran with #getPlugin() != null instead of #isPluginEnabled() as older Spigot builds return false during the login process even if enabled
            boolean viaVersionExists = adapter.isPluginEnabled("ViaVersion");
            boolean protocolSupportExists = adapter.isPluginEnabled("ProtocolSupport");
            if (viaVersionExists || protocolSupportExists) {
                logger.debug(SRLogLevel.WARNING, "Unsupported plugin (ViaVersion or ProtocolSupport) detected, forcing SpigotSkinRefresher");
                return selectSpigotRefresher(server);
            }

            // use PaperSkinRefresher if no VersionHack plugin found
            try {
                logger.debug("Using PaperSkinRefresher");
                return new PaperSkinRefresher();
            } catch (InitializeException e) {
                logger.severe("PaperSkinRefresher failed! (Are you using hybrid software?) Only limited support can be provided. Falling back to SpigotSkinRefresher.");
            }
        }

        return selectSpigotRefresher(server);
    }

    private SkinRefresher selectSpigotRefresher(Server server) throws InitializeException, NoMappingException {
        // Wait to run task in order for ViaVersion to determine server protocol
        boolean viaWorkaround = adapter.isPluginEnabled("ViaBackwards")
                && ViaWorkaround.isProtocolNewer();

        if (viaWorkaround) {
            logger.debug("Activating ViaBackwards workaround.");
        }

        if (BukkitReflection.SERVER_VERSION.isNewerThan(new SemanticVersion(1, 17, 1))) {
            logger.debug("Using MappingSpigotSkinRefresher");
            return new MappingSpigotSkinRefresher(server, viaWorkaround);
        } else {
            logger.debug("Using SpigotSkinRefresher");
            return new SpigotSkinRefresher(adapter, logger, viaWorkaround);
        }
    }

    private boolean isPaper() {
        if (ClassInfo.get().isPaper() && BukkitReflection.SERVER_VERSION.isNewerThan(new SemanticVersion(1, 11, 2))) {
            if (hasPaperMethods()) {
                return true;
            } else {
                logger.debug(SRLogLevel.WARNING, "Paper detected, but the methods are missing. Disabling Paper Refresher.");
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean hasPaperMethods() {
        try {
            BukkitReflection.getBukkitClass("entity.CraftPlayer").getDeclaredMethod("refreshPlayer");
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    @Override
    public void initLoginProfileListener() {
        if (PaperPlayerJoinEvent.isAvailable() && settingsManager.getProperty(AdvancedConfig.ENABLE_PAPER_JOIN_LISTENER)) {
            logger.info("Using paper join listener!");
            server.getPluginManager().registerEvents(injector.newInstance(PaperPlayerJoinEvent.class), adapter.getPluginInstance());
        } else {
            server.getPluginManager().registerEvents(injector.newInstance(PlayerJoinListener.class), adapter.getPluginInstance());

            if (ReflectionUtil.classExists("org.bukkit.event.player.PlayerResourcePackStatusEvent")) {
                server.getPluginManager().registerEvents(injector.newInstance(PlayerResourcePackStatusListener.class), adapter.getPluginInstance());
            }
        }
    }

    @Override
    public void prePlatformInit() {
        server.getHelpMap().registerHelpTopicFactory(SRBukkitCommand.class, command ->
                new SRHelpTopic((SRBukkitCommand) command, wrapper, locale));

        // Shutdown kyori adventure
        plugin.getShutdownHooks().add(() -> adapter.getAdventure().close());
    }

    @Override
    public void checkPluginSupport() {
        checkViaVersion();

        checkMundoSK();
    }

    private void checkViaVersion() {
        if (!adapter.isPluginEnabled("ViaVersion")) {
            return;
        }

        // ViaVersion 4.0.0+ class
        if (ReflectionUtil.classExists("com.viaversion.viaversion.api.Via")) {
            return;
        }

        adapter.runRepeatAsync(() -> logger.severe("Outdated ViaVersion found! Please update to at least ViaVersion 4.0.0 for SkinsRestorer to work again!"),
                2, 60, TimeUnit.SECONDS);
    }

    private void checkMundoSK() {
        if (!adapter.isPluginEnabled("MundoSK")) {
            return;
        }

        Path pluginsFolder = plugin.getDataFolder().getParent();
        if (pluginsFolder == null) { // Unlikely to happen, but just in case
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(pluginsFolder.resolve("MundoSK").resolve("config.yml"))) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(reader);
            if (config.getBoolean("enable_custom_skin_and_tablist")) {
                logger.warning(SRChatColor.DARK_RED + "----------------------------------------------");
                logger.warning(SRChatColor.DARK_RED + "             [CRITICAL WARNING]");
                logger.warning(SRChatColor.RED + "We have detected MundoSK on your server with " + SRChatColor.YELLOW + "'enable_custom_skin_and_tablist: " + SRChatColor.DARK_RED + SRChatColor.UNDERLINE + "true" + SRChatColor.YELLOW + "' " + SRChatColor.RED + ".");
                logger.warning(SRChatColor.RED + "That setting is located in §e/plugins/MundoSK/config.yml");
                logger.warning(SRChatColor.RED + "You have to disable ('false') it to get SkinsRestorer to work!");
                logger.warning(SRChatColor.DARK_RED + "----------------------------------------------");
            }
        } catch (IOException e) {
            logger.warning("Could not read MundoSK config.yml to check for 'enable_custom_skin_and_tablist'!", e);
        }
    }

    @Override
    public void initPermissions() {
        for (PermissionRegistry permission : PermissionRegistry.values()) {
            String permissionString = permission.getPermission().getPermissionString();
            String description = ComponentHelper.convertJsonToLegacy(locale.getMessageRequired(locale.getDefaultForeign(), permission.getDescription()));

            addPermission(new Permission(permissionString, description));
        }

        for (PermissionGroup group : PermissionGroup.values()) {
            String description = ComponentHelper.convertJsonToLegacy(locale.getMessageRequired(locale.getDefaultForeign(), group.getDescription()));
            Map<String, Boolean> children = new HashMap<>();
            mergePermissions(group, children);
            PermissionDefault permissionDefault = group == PermissionGroup.PLAYER ? PermissionDefault.TRUE : PermissionDefault.OP;

            addPermission(new Permission(group.getBasePermission().getPermissionString(), description, permissionDefault, children));
            addPermission(new Permission(group.getWildcard().getPermissionString(), description, permissionDefault, children));
        }
    }

    private void addPermission(Permission permission) {
        SimplePluginManager pluginManager = (SimplePluginManager) server.getPluginManager();

        if (pluginManager.getPermission(permission.getName()) != null) {
            return;
        }

        pluginManager.addPermission(permission);
    }

    private void mergePermissions(PermissionGroup group, Map<String, Boolean> data) {
        for (PermissionRegistry permission : group.getPermissions()) {
            data.put(permission.getPermission().getPermissionString(), true);
        }

        for (PermissionGroup childGroup : group.getParents()) {
            mergePermissions(childGroup, data);
        }
    }

    @Override
    public void initGUIListener() {
        server.getPluginManager().registerEvents(injector.getSingleton(InventoryListener.class), adapter.getPluginInstance());
    }

    @Override
    public void initMessageChannel() {
        server.getMessenger().registerOutgoingPluginChannel(adapter.getPluginInstance(), SRConstants.MESSAGE_CHANNEL);
        server.getMessenger().registerIncomingPluginChannel(adapter.getPluginInstance(), SRConstants.MESSAGE_CHANNEL,
                injector.getSingleton(ServerMessageListener.class));
    }

    @Override
    public void postAPIInitHook() {
        if (adapter.isPluginEnabled("PlaceholderAPI")) {
            new SRPlaceholderAPIExpansion(
                    SkinsRestorerProvider.get(),
                    logger,
                    adapter.getPluginInstance().getDescription(),
                    injector
            ).register();
            logger.info("PlaceholderAPI expansion registered!");
        }
    }
}
