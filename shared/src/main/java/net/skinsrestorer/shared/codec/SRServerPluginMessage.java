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
package net.skinsrestorer.shared.codec;

import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.gui.SRInventory;

import java.util.HashMap;
import java.util.Map;

public record SRServerPluginMessage(ChannelPayload<?> channelPayload) {
    public static final NetworkCodec<SRServerPluginMessage> CODEC = NetworkCodec.of(
            (out, msg) -> {
                ChannelType.CODEC.write(out, msg.channelPayload().getType());
                msg.channelPayload().writeCodec(out);
            },
            in -> new SRServerPluginMessage(ChannelType.CODEC.read(in).codec().read(in))
    );

    public record ChannelType<T extends ChannelPayload<T>>(String channelName, NetworkCodec<T> codec) implements NetworkId {
        private static final Map<String,ChannelType<?>> ID_TO_VALUE = new HashMap<>();

        public static final ChannelType<GUIPageChannelPayload> OPEN_GUI = register(new ChannelType<>("openGUI", GUIPageChannelPayload.CODEC));
        public static final ChannelType<SkinUpdateChannelPayload> SKIN_UPDATE = register(new ChannelType<>("SkinUpdateV2", SkinUpdateChannelPayload.CODEC));

        public static final NetworkCodec<ChannelType<?>> CODEC = NetworkCodec.ofMapBackedDynamic(ID_TO_VALUE, NetworkId::getId,
                "Unknown channel type: %s (Make sure the server and proxy are running the same version of SkinsRestorer)"::formatted);

        private static <T extends ChannelPayload<T>> ChannelType<T> register(ChannelType<T> channelType) {
            ID_TO_VALUE.put(channelType.getId(), channelType);
            return channelType;
        }

        @Override
        public String getId() {
            return channelName;
        }
    }

    public interface ChannelPayload<T extends ChannelPayload<T>> {
        ChannelType<T> getType();

        T cast();

        default void writeCodec(SROutputWriter out) {
            getType().codec().write(out, cast());
        }
    }

    public record GUIPageChannelPayload(SRInventory srInventory) implements ChannelPayload<GUIPageChannelPayload> {
        public static final NetworkCodec<GUIPageChannelPayload> CODEC = NetworkCodec.of(
                (out, msg) -> SRInventory.CODEC.write(out, msg.srInventory()),
                in -> new GUIPageChannelPayload(SRInventory.CODEC.read(in))
        );

        @Override
        public ChannelType<GUIPageChannelPayload> getType() {
            return ChannelType.OPEN_GUI;
        }

        @Override
        public GUIPageChannelPayload cast() {
            return this;
        }
    }

    public record SkinUpdateChannelPayload(
            SkinProperty skinProperty) implements ChannelPayload<SkinUpdateChannelPayload> {
        public static final NetworkCodec<SkinUpdateChannelPayload> CODEC = NetworkCodec.of(
                (out, msg) -> BuiltInCodecs.SKIN_PROPERTY_CODEC.write(out, msg.skinProperty()),
                in -> new SkinUpdateChannelPayload(BuiltInCodecs.SKIN_PROPERTY_CODEC.read(in))
        );

        @Override
        public ChannelType<SkinUpdateChannelPayload> getType() {
            return ChannelType.SKIN_UPDATE;
        }

        @Override
        public SkinUpdateChannelPayload cast() {
            return this;
        }
    }
}
