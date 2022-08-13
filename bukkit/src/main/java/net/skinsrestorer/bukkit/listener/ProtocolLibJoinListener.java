/*
 * SkinsRestorer
 *
 * Copyright (C) 2022 SkinsRestorer
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
package net.skinsrestorer.bukkit.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.bukkit.listener.protocol.WrapperPlayServerPlayerInfo;
import net.skinsrestorer.shared.interfaces.ISRPlugin;
import net.skinsrestorer.shared.listeners.LoginProfileListener;
import net.skinsrestorer.shared.storage.Config;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;

public class ProtocolLibJoinListener extends LoginProfileListener { // TODO: implement listener methods
    @Getter
    private final SkinsRestorer plugin;

    public ProtocolLibJoinListener(SkinsRestorer skinsRestorer) {
        this.plugin = skinsRestorer;
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(skinsRestorer, ListenerPriority.LOWEST, ImmutableList.of(PacketType.Play.Server.PLAYER_INFO), ListenerOptions.ASYNC) {
            @Override
            public void onPacketSending(PacketEvent event) {
                WrapperPlayServerPlayerInfo wrapper = new WrapperPlayServerPlayerInfo(event.getPacket());

                if (wrapper.getAction() != EnumWrappers.PlayerInfoAction.ADD_PLAYER)
                    return;

                List<PlayerInfoData> list = wrapper.getData();
                if (list.isEmpty())
                    return;

                PlayerInfoData data = list.get(0);
                String targetName = data.getProfile().getName();
                Player targetPlayer = skinsRestorer.getServer().getPlayer(targetName);

                if (targetPlayer == null)
                    return;

                if (targetPlayer.hasMetadata("skinsrestorer.appliedOnJoin"))
                    return;

                try {
                    IProperty property = skinsRestorer.getSkinStorage().getDefaultSkinForPlayer(targetName).getLeft();

                    skinsRestorer.getSkinApplierBukkit().applyProperty(targetPlayer, property);

                    targetPlayer.setMetadata("skinsrestorer.appliedOnJoin", new FixedMetadataValue(plugin, true));

                    data.getProfile().getProperties().removeAll(IProperty.TEXTURES_NAME);
                    data.getProfile().getProperties().put(IProperty.TEXTURES_NAME, new WrappedSignedProperty(property.getName(), property.getValue(), property.getSignature()));
                    wrapper.setData(list);
                } catch (SkinRequestException ignored) {
                }
            }
        });
    }
}
