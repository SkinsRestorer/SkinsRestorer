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
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.shared.gui.PageType;

import java.util.List;

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
        GUI_ACTION_LIST("guiActionList", GUIActionChannelPayloadList.CODEC);

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

    public record GUIActionChannelPayloadList(List<GUIActionChannelPayload> actions) implements ChannelPayload<GUIActionChannelPayloadList> {
        public static final NetworkCodec<GUIActionChannelPayloadList> CODEC = NetworkCodec.of(
                (out, msg) -> GUIActionChannelPayload.CODEC.list().write(out, msg.actions),
                in -> new GUIActionChannelPayloadList(GUIActionChannelPayload.CODEC.list().read(in))
        );

        @Override
        public ChannelType getType() {
            return ChannelType.GUI_ACTION_LIST;
        }

        @Override
        public NetworkCodec<GUIActionChannelPayloadList> getCodec() {
            return CODEC;
        }

        @Override
        public GUIActionChannelPayloadList cast() {
            return this;
        }
    }

    public record GUIActionChannelPayload(GUIActionPayload<?> payload) {
        public static final NetworkCodec<GUIActionChannelPayload> CODEC = NetworkCodec.of(
                (out, msg) -> {
                    GUIActionType.CODEC.write(out, msg.payload().getType());
                    msg.payload().writeCodec(out);
                },
                in -> new GUIActionChannelPayload(GUIActionType.CODEC.read(in).getCodec().read(in))
        );

        @Getter
        @RequiredArgsConstructor
        public enum GUIActionType implements NetworkId {
            OPEN_PAGE("openPage", OpenPagePayload.CODEC),
            CLEAR_SKIN("clearSkin", ClearSkinPayload.CODEC),
            SET_SKIN("setSkin", SetSkinPayload.CODEC),
            ADD_FAVOURITE("addFavourite", AddFavouritePayload.CODEC),
            REMOVE_FAVOURITE("removeFavourite", RemoveFavouritePayload.CODEC);

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

        public record SetSkinPayload(SkinIdentifier skinIdentifier) implements GUIActionPayload<SetSkinPayload> {
            public static final NetworkCodec<SetSkinPayload> CODEC = NetworkCodec.of(
                    (out, msg) -> BuiltInCodecs.SKIN_IDENTIFIER_CODEC.write(out, msg.skinIdentifier()),
                    in -> new SetSkinPayload(BuiltInCodecs.SKIN_IDENTIFIER_CODEC.read(in))
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

        public record AddFavouritePayload(
                SkinIdentifier skinIdentifier) implements GUIActionPayload<AddFavouritePayload> {
            public static final NetworkCodec<AddFavouritePayload> CODEC = NetworkCodec.of(
                    (out, msg) -> BuiltInCodecs.SKIN_IDENTIFIER_CODEC.write(out, msg.skinIdentifier()),
                    in -> new AddFavouritePayload(BuiltInCodecs.SKIN_IDENTIFIER_CODEC.read(in))
            );

            @Override
            public GUIActionType getType() {
                return GUIActionType.SET_SKIN;
            }

            @Override
            public NetworkCodec<AddFavouritePayload> getCodec() {
                return CODEC;
            }

            @Override
            public AddFavouritePayload cast() {
                return this;
            }
        }

        public record RemoveFavouritePayload(
                SkinIdentifier skinIdentifier) implements GUIActionPayload<RemoveFavouritePayload> {
            public static final NetworkCodec<RemoveFavouritePayload> CODEC = NetworkCodec.of(
                    (out, msg) -> BuiltInCodecs.SKIN_IDENTIFIER_CODEC.write(out, msg.skinIdentifier()),
                    in -> new RemoveFavouritePayload(BuiltInCodecs.SKIN_IDENTIFIER_CODEC.read(in))
            );

            @Override
            public GUIActionType getType() {
                return GUIActionType.SET_SKIN;
            }

            @Override
            public NetworkCodec<RemoveFavouritePayload> getCodec() {
                return CODEC;
            }

            @Override
            public RemoveFavouritePayload cast() {
                return this;
            }
        }
    }
}
