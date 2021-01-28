/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.velocity.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.annotation.*;
import co.aikar.commands.velocity.contexts.OnlinePlayer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.shared.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.velocity.SkinsRestorer;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
@CommandAlias("skin")
@CommandPermission("%skin")
public class SkinCommand extends BaseCommand {
    private final SkinsRestorer plugin;

    public SkinCommand(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @Default
    public void onDefault(CommandSource source) {
        this.onHelp(source, this.getCurrentCommandManager().generateCommandHelp());
    }

    @Default
    @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxDefaultCommand")
    public void onSkinSetShort(Player p, @Single String skin) {
        this.onSkinSetOther(p, new OnlinePlayer(p), skin);
    }

    @HelpCommand
    @Syntax(" [help]")
    public void onHelp(CommandSource commandSource, CommandHelp help) {
        if (Config.USE_OLD_SKIN_HELP)
            sendHelp(commandSource);
        else
            help.showHelp();
    }


    @Subcommand("clear")
    @CommandPermission("%skinClear")
    @Description("%helpSkinClear")
    public void onSkinClear(Player p) {
        this.onSkinClearOther(p, new OnlinePlayer(p));
    }

    @Subcommand("clear")
    @CommandPermission("%skinClearOther")
    @CommandCompletion("@players")
    @Description("%helpSkinClearOther")
    public void onSkinClearOther(CommandSource source, OnlinePlayer target) {
        plugin.getService().execute(() -> {
            Player p = target.getPlayer();
            String skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(p.getUsername(), true);

            // remove users custom skin and set default skin / his skin
            plugin.getSkinStorage().removePlayerSkin(p.getUsername());
            if (this.setSkin(source, p, skin, false)) {
                if (!getSenderName(source).equals(target.getPlayer().getUsername()))
                    source.sendMessage(plugin.deserialize(Locale.SKIN_CLEAR_ISSUER.replace("%player", target.getPlayer().getUsername())));
                else
                    source.sendMessage(plugin.deserialize(Locale.SKIN_CLEAR_SUCCESS));
            }
        });
    }


    @Subcommand("update")
    @CommandPermission("%skinUpdate")
    @Description("%helpSkinUpdate")
    public void onSkinUpdate(Player p) {
        this.onSkinUpdateOther(p, new OnlinePlayer(p));
    }

    @Subcommand("update")
    @CommandPermission("%skinUpdateOther")
    @CommandCompletion("@players")
    @Description("%helpSkinUpdateOther")
    @Syntax("%SyntaxSkinUpdateOther")
    public void onSkinUpdateOther(CommandSource source, OnlinePlayer target) {
        plugin.getService().execute(() -> {
            //Check cooldown first
            if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(getSenderName(source))) {
                source.sendMessage(plugin.deserialize(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(getSenderName(source)))));
                return;
            }

            Player p = target.getPlayer();
            String skin = plugin.getSkinStorage().getPlayerSkin(p.getUsername());

            try {
                if (skin != null) {
                    //filter skinUrl
                    if (skin.contains(" ")) {
                        source.sendMessage(plugin.deserialize(Locale.ERROR_UPDATING_URL));
                        return;
                    }
                    // check if premium name
                    plugin.getMojangAPI().getUUIDMojang(skin);
                    if (!plugin.getSkinStorage().forceUpdateSkinData(skin)) {
                        source.sendMessage(plugin.deserialize(Locale.ERROR_UPDATING_SKIN));
                        return;
                    }

                } else {
                    // get DefaultSkin
                    skin = plugin.getSkinStorage().getDefaultSkinNameIfEnabled(p.getUsername(), true);
                }
            } catch (SkinRequestException e) {
                // non premium = cancel
                source.sendMessage(plugin.deserialize(Locale.ERROR_UPDATING_CUSTOMSKIN));
                return;
            }

            if (this.setSkin(source, p, skin, false)) {
                if (!getSenderName(source).equals(p.getUsername()))
                    source.sendMessage(plugin.deserialize(Locale.SUCCESS_UPDATING_SKIN_OTHER.replace("%player", p.getUsername())));
                else
                    source.sendMessage(plugin.deserialize(Locale.SUCCESS_UPDATING_SKIN));
            }
        });
    }

    @Subcommand("set")
    @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxSkinSet")
    public void onSkinSet(Player p, String[] skin) {
        if (skin.length > 0) {
            this.onSkinSetOther(p, new OnlinePlayer(p), skin[0]);
        } else {
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        }
    }

    @Subcommand("set")
    @CommandPermission("%skinSetOther")
    @CommandCompletion("@players")
    @Description("%helpSkinSetOther")
    @Syntax("%SyntaxSkinSetOther")
    public void onSkinSetOther(CommandSource source, OnlinePlayer target, String skin) {
        if (Config.PER_SKIN_PERMISSIONS
                && !source.hasPermission("skinsrestorer.skin." + skin)
                && !getSenderName(source).equals(target.getPlayer().getUsername()) || (!source.hasPermission("skinsrestorer.ownskin") && !skin.equalsIgnoreCase(getSenderName(source)))) {
            source.sendMessage(LegacyComponentSerializer.legacy().deserialize(Locale.PLAYER_HAS_NO_PERMISSION_SKIN));
            return;
        }

        plugin.getService().execute(() -> {
            if (this.setSkin(source, target.getPlayer(), skin) && !getSenderName(source).equals(target.getPlayer().getUsername())) {
                source.sendMessage(LegacyComponentSerializer.legacy().deserialize(Locale.ADMIN_SET_SKIN.replace("%player", target.getPlayer().getUsername())));
            }
        });
    }

    @Subcommand("url")
    @CommandPermission("%skinSetUrl")
    @Description("%helpSkinSetUrl")
    @Syntax("%SyntaxSkinUrl")
    public void onSkinSetUrl(Player p, String[] url) {
        if (url.length > 0) {
            if (C.validUrl(url[0])) {
                this.onSkinSetOther(p, new OnlinePlayer(p), url[0]);
            } else {
                p.sendMessage(LegacyComponentSerializer.legacy().deserialize(Locale.ERROR_INVALID_URLSKIN_2));
            }
        } else {
            throw new InvalidCommandArgument(MessageKeys.INVALID_SYNTAX);
        }
    }

    private boolean setSkin(CommandSource source, Player p, String skin) {
        return this.setSkin(source, p, skin, true);
    }

    // if save is false, we won't save the skin skin name
    // because default skin names shouldn't be saved as the users custom skin
    private boolean setSkin(CommandSource source, Player p, String skin, boolean save) {
        if (!C.validUsername(skin) && !C.validUrl(skin)) {
            if (C.matchesRegex(skin)) {
                source.sendMessage(plugin.deserialize(Locale.ERROR_INVALID_URLSKIN_2));
            } else {
                source.sendMessage(plugin.deserialize(Locale.INVALID_PLAYER.replace("%player", skin)));
            }

            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED && !source.hasPermission("skinsrestorer.bypassdisabled")) {
            for (String dskin : Config.DISABLED_SKINS)
                if (skin.equalsIgnoreCase(dskin)) {
                    source.sendMessage(plugin.deserialize(Locale.SKIN_DISABLED));
                    return false;
                }
        }

        if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(getSenderName(source))) {
            source.sendMessage(plugin.deserialize(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(getSenderName(source)))));
            return false;
        }

        CooldownStorage.resetCooldown(getSenderName(source));
        CooldownStorage.setCooldown(getSenderName(source), Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

        String oldSkinName = plugin.getSkinStorage().getPlayerSkin(p.getUsername());
        plugin.getService().execute(() -> {
            if (C.validUsername(skin)) {
                try {
                    plugin.getSkinStorage().getOrCreateSkinForPlayer(skin, false);
                    if (save) {
                        plugin.getSkinStorage().setPlayerSkin(p.getUsername(), skin);
                        plugin.getSkinApplierVelocity().applySkin(new PlayerWrapper(p), plugin.getSkinsRestorerVelocityAPI());
                    } else {
                        plugin.getSkinApplierVelocity().applySkin(new PlayerWrapper(p), plugin.getSkinsRestorerVelocityAPI());
                    }
                    p.sendMessage(plugin.deserialize(Locale.SKIN_CHANGE_SUCCESS));
                } catch (SkinRequestException e) {
                    source.sendMessage(plugin.deserialize(e.getMessage()));
                    // set custom skin name back to old one if there is an exception
                    this.rollback(p, oldSkinName, save);
                } catch (Exception e) {
                    e.printStackTrace();
                    // set custom skin name back to old one if there is an exception
                    this.rollback(p, oldSkinName, save);
                }
            }
            if (C.validUrl(skin)) {
                if (!source.hasPermission("skinsrestorer.command.set.url") && !Config.SKINWITHOUTPERM) {
                    source.sendMessage(plugin.deserialize(Locale.PLAYER_HAS_NO_PERMISSION_URL));
                    return;
                }

                try {
                    source.sendMessage(plugin.deserialize(Locale.MS_UPDATING_SKIN));
                    String skinentry = " " + p.getUsername(); // so won't overwrite premium playernames
                    if (skinentry.length() > 16) // max len of 16 char
                        skinentry = skinentry.substring(0, 16);
                    plugin.getSkinStorage().setSkinData(skinentry, plugin.getMineSkinAPI().genSkin(skin),
                            Long.toString(System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000))); // "generate" and save skin for 100 years
                    plugin.getSkinStorage().setPlayerSkin(p.getUsername(), skinentry); // set player to "whitespaced" name then reload skin
                    plugin.getSkinApplierVelocity().applySkin(new PlayerWrapper(p), plugin.getSkinsRestorerVelocityAPI());
                    p.sendMessage(plugin.deserialize(Locale.SKIN_CHANGE_SUCCESS));
                } catch (SkinRequestException e) {
                    source.sendMessage(plugin.deserialize(e.getMessage()));
                    // set custom skin name back to old one if there is an exception
                    this.rollback(p, oldSkinName, save);
                } catch (Exception e) {
                    e.printStackTrace();
                    // set custom skin name back to old one if there is an exception
                    this.rollback(p, oldSkinName, save);
                }
            }
        });
        return true;
    }

    private void rollback(Player p, String oldSkinName, boolean save) {
        if (save)
            plugin.getSkinStorage().setPlayerSkin(p.getUsername(), oldSkinName != null ? oldSkinName : p.getUsername());
    }

    private void sendHelp(CommandSource commandSource) {
        if (!Locale.SR_LINE.isEmpty())
            commandSource.sendMessage(plugin.deserialize(Locale.SR_LINE));
        commandSource.sendMessage(plugin.deserialize(Locale.HELP_PLAYER.replace("%ver%", plugin.getVersion())));
        if (!Locale.SR_LINE.isEmpty())
            commandSource.sendMessage(plugin.deserialize(Locale.SR_LINE));
    }

    private String getSenderName(CommandSource source) {
        return source instanceof Player ? ((Player) source).getUsername() : "CONSOLE";
    }
}
