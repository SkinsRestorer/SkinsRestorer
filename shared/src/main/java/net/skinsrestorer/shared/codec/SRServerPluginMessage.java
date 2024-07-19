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
                CodecHelpers.STRING_CODEC.write(out, msg.channelPayload().getType().getChannelName());
                msg.channelPayload().writeCodec(out);
            },
            in -> {
                String channelName = CodecHelpers.STRING_CODEC.read(in);
                ChannelType type = ChannelType.fromName(channelName);
                return new SRServerPluginMessage(type.getCodec().read(in));
            }
    );

    @Getter
    @RequiredArgsConstructor
    public enum ChannelType {
        OPEN_GUI("openGUI", GUIPageChannelPayload.CODEC),
        SKIN_UPDATE("SkinUpdateV2", GUIPageChannelPayload.CODEC);

        private final String channelName;
        private final NetworkCodec<? extends ChannelPayload<?>> codec;

        public static ChannelType fromName(String name) {
            for (ChannelType type : values()) {
                if (type.getChannelName().equals(name)) {
                    return type;
                }
            }

            throw new IllegalArgumentException("Unknown channel type: " + name);
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
                (out, msg) -> {
                    SRInventory.CODEC.write(out, msg.srInventory());
                },
                in -> {
                    SRInventory srInventory = SRInventory.CODEC.read(in);
                    return new GUIPageChannelPayload(srInventory);
                }
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
                (out, msg) -> {
                    CodecHelpers.SKIN_PROPERTY_CODEC.write(out, msg.skinProperty());
                },
                in -> {
                    SkinProperty skinProperty = CodecHelpers.SKIN_PROPERTY_CODEC.read(in);
                    return new SkinUpdateChannelPayload(skinProperty);
                }
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
