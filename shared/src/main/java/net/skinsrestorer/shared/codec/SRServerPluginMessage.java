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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.shared.gui.SRInventory;

public record SRServerPluginMessage(ChannelPayload<?> channelPayload) {
    public static final NetworkCodec<SRServerPluginMessage> CODEC = NetworkCodec.of(
            (out, msg) -> {
                ChannelType.CODEC.write(out, msg.channelPayload().getType());
                msg.channelPayload().writeCodec(out);
            },
            in -> new SRServerPluginMessage(ChannelType.CODEC.read(in).getCodec().read(in))
    );

    @Getter
    @RequiredArgsConstructor
    public enum ChannelType implements NetworkId {
        OPEN_GUI("openGUI", GUIPageChannelPayload.CODEC),
        SKIN_UPDATE("SkinUpdateV2", GUIPageChannelPayload.CODEC);

        public static final NetworkCodec<ChannelType> CODEC = CodecHelpers.createEnumCodec(ChannelType.class);
        private final String channelName;
        private final NetworkCodec<? extends ChannelPayload<?>> codec;

        @Override
        public String getId() {
            return channelName;
        }
    }

    public interface ChannelPayload<T extends ChannelPayload<T>> {
        ChannelType getType();

        NetworkCodec<T> getCodec();

        T cast();

        default void writeCodec(SROutputWriter out) {
            getCodec().write(out, cast());
        }
    }

    public record GUIPageChannelPayload(SRInventory srInventory) implements ChannelPayload<GUIPageChannelPayload> {
        public static final NetworkCodec<GUIPageChannelPayload> CODEC = NetworkCodec.of(
                (out, msg) -> SRInventory.CODEC.write(out, msg.srInventory()),
                in -> new GUIPageChannelPayload(SRInventory.CODEC.read(in))
        );

        @Override
        public ChannelType getType() {
            return ChannelType.OPEN_GUI;
        }

        @Override
        public NetworkCodec<GUIPageChannelPayload> getCodec() {
            return CODEC;
        }

        @Override
        public GUIPageChannelPayload cast() {
            return this;
        }
    }

    public record SkinUpdateChannelPayload(
            SkinProperty skinProperty) implements ChannelPayload<SkinUpdateChannelPayload> {
        public static final NetworkCodec<SkinUpdateChannelPayload> CODEC = NetworkCodec.of(
                (out, msg) -> CodecHelpers.SKIN_PROPERTY_CODEC.write(out, msg.skinProperty()),
                in -> new SkinUpdateChannelPayload(CodecHelpers.SKIN_PROPERTY_CODEC.read(in))
        );

        @Override
        public ChannelType getType() {
            return ChannelType.OPEN_GUI;
        }

        @Override
        public NetworkCodec<SkinUpdateChannelPayload> getCodec() {
            return CODEC;
        }

        @Override
        public SkinUpdateChannelPayload cast() {
            return this;
        }
    }
}
