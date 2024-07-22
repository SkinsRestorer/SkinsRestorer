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
import net.skinsrestorer.shared.gui.PageType;

public record SRProxyPluginMessage(ChannelPayload<?> channelPayload) {
    public static final NetworkCodec<SRProxyPluginMessage> CODEC = NetworkCodec.of(
            (out, msg) -> {
                ChannelType.CODEC.write(out, msg.channelPayload().getType());
                msg.channelPayload().writeCodec(out);
            },
            in -> new SRProxyPluginMessage(ChannelType.CODEC.read(in).getCodec().read(in))
    );

    @Getter
    @RequiredArgsConstructor
    public enum ChannelType implements NetworkId {
        GUI_ACTION("guiAction", GUIActionChannelPayload.CODEC);

        public static final NetworkCodec<ChannelType> CODEC = NetworkCodec.ofEnum(ChannelType.class,
                "Unknown channel type: %s (Make sure the server and proxy are running the same version of SkinsRestorer)"::formatted);
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

    public record GUIActionChannelPayload(
            GUIActionPayload<?> payload) implements ChannelPayload<GUIActionChannelPayload> {
        public static final NetworkCodec<GUIActionChannelPayload> CODEC = NetworkCodec.of(
                (out, msg) -> {
                    GUIActionType.CODEC.write(out, msg.payload().getType());
                    msg.payload().writeCodec(out);
                },
                in -> new GUIActionChannelPayload(GUIActionType.CODEC.read(in).getCodec().read(in))
        );

        @Override
        public ChannelType getType() {
            return ChannelType.GUI_ACTION;
        }

        @Override
        public NetworkCodec<GUIActionChannelPayload> getCodec() {
            return CODEC;
        }

        @Override
        public GUIActionChannelPayload cast() {
            return this;
        }

        @Getter
        @RequiredArgsConstructor
        public enum GUIActionType implements NetworkId {
            OPEN_PAGE("openPage", OpenPagePayload.CODEC),
            CLEAR_SKIN("clearSkin", ClearSkinPayload.CODEC),
            SET_SKIN("setSkin", SetSkinPayload.CODEC);

            public static final NetworkCodec<GUIActionType> CODEC = NetworkCodec.ofEnum(GUIActionType.class);
            private final String channelName;
            private final NetworkCodec<? extends GUIActionPayload<?>> codec;

            @Override
            public String getId() {
                return channelName;
            }
        }

        public interface GUIActionPayload<T extends GUIActionPayload<T>> {
            GUIActionType getType();

            NetworkCodec<T> getCodec();

            T cast();

            default void writeCodec(SROutputWriter out) {
                getCodec().write(out, cast());
            }
        }

        public record OpenPagePayload(int page, PageType type) implements GUIActionPayload<OpenPagePayload> {
            public static final NetworkCodec<OpenPagePayload> CODEC = NetworkCodec.of(
                    (out, msg) -> {
                        BuiltInCodecs.INT_CODEC.write(out, msg.page());
                        PageType.CODEC.write(out, msg.type());
                    },
                    in -> new OpenPagePayload(BuiltInCodecs.INT_CODEC.read(in), PageType.CODEC.read(in))
            );

            @Override
            public GUIActionType getType() {
                return GUIActionType.OPEN_PAGE;
            }

            @Override
            public NetworkCodec<OpenPagePayload> getCodec() {
                return CODEC;
            }

            @Override
            public OpenPagePayload cast() {
                return this;
            }
        }

        public record ClearSkinPayload() implements GUIActionPayload<ClearSkinPayload> {
            public static final NetworkCodec<ClearSkinPayload> CODEC = NetworkCodec.of(
                    (out, msg) -> {
                    },
                    in -> new ClearSkinPayload()
            );

            @Override
            public GUIActionType getType() {
                return GUIActionType.CLEAR_SKIN;
            }

            @Override
            public NetworkCodec<ClearSkinPayload> getCodec() {
                return CODEC;
            }

            @Override
            public ClearSkinPayload cast() {
                return this;
            }
        }

        public record SetSkinPayload(String skin) implements GUIActionPayload<SetSkinPayload> {
            public static final NetworkCodec<SetSkinPayload> CODEC = NetworkCodec.of(
                    (out, msg) -> BuiltInCodecs.STRING_CODEC.write(out, msg.skin()),
                    in -> new SetSkinPayload(BuiltInCodecs.STRING_CODEC.read(in))
            );

            @Override
            public GUIActionType getType() {
                return GUIActionType.SET_SKIN;
            }

            @Override
            public NetworkCodec<SetSkinPayload> getCodec() {
                return CODEC;
            }

            @Override
            public SetSkinPayload cast() {
                return this;
            }
        }
    }
}
