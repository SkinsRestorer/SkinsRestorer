/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.bukkit.SkinsRestorer;
import net.skinsrestorer.bukkit.listener.protocol.WrapperPlayServerPlayerInfo;
import net.skinsrestorer.shared.storage.Config;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;

public class ProtocolLibJoinListener {
    public ProtocolLibJoinListener(SkinsRestorer skinsRestorer) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(skinsRestorer, ListenerPriority.LOWEST, PacketType.Play.Server.PLAYER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (Config.DISABLE_ON_JOIN_SKINS)
                    return;

                WrapperPlayServerPlayerInfo wrapper = new WrapperPlayServerPlayerInfo(event.getPacket());

                if (wrapper.getAction() != EnumWrappers.PlayerInfoAction.ADD_PLAYER)
                    return;

                List<PlayerInfoData> list = wrapper.getData();
                if (list.size() < 1)
                    return;

                PlayerInfoData data = list.get(0);
                String targetName = data.getProfile().getName();
                Player targetPlayer = plugin.getServer().getPlayer(targetName);

                if (targetPlayer == null)
                    return;

                if (targetPlayer.hasMetadata("skinsrestorer.appliedOnJoin"))
                    return;

                try {
                    IProperty property = skinsRestorer.getSkinStorage().getDefaultSkinForPlayer(targetName);

                    skinsRestorer.getSkinApplierBukkit().applyProperty(targetPlayer, property);

                    targetPlayer.setMetadata("skinsrestorer.appliedOnJoin", new FixedMetadataValue(skinsRestorer, true));

                    data.getProfile().getProperties().removeAll("textures");
                    data.getProfile().getProperties().put("textures", new WrappedSignedProperty(property.getName(), property.getValue(), property.getSignature()));
                    list.set(0, new PlayerInfoData(data.getProfile(), data.getLatency(), data.getGameMode(), data.getDisplayName()));
                    wrapper.setData(list);
                } catch (SkinRequestException ignored) {
                }
            }
        });
    }
}
