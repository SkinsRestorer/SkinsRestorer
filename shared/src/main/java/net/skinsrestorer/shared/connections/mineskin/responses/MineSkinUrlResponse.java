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
package net.skinsrestorer.shared.connections.mineskin.responses;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.ToString;
import net.skinsrestorer.shared.connections.mineskin.MineSkinVariant;
import net.skinsrestorer.shared.connections.mineskin.MineSkinVisibility;

import java.util.List;
import java.util.Map;

@ToString
@Getter
@SuppressWarnings("unused")
@SuppressFBWarnings({"UWF_UNWRITTEN_FIELD", "URF_UNREAD_FIELD"})
public class MineSkinUrlResponse {
    private Skin skin;
    private RateLimit rateLimit;
    private Usage usage;
    private boolean success;
    private List<Error> errors;
    private List<Warning> warnings;
    private List<Message> messages;
    private Map<String, String> links;

    @ToString
    @Getter
    public static class Skin {
        private String uuid;
        private String name;
        private MineSkinVisibility visibility;
        private MineSkinVariant variant;
        private Texture texture;
        private Generator generator;
        private int views;
        private boolean duplicate;

        @ToString
        @Getter
        public static class Texture {
            private Data data;
            private Hash hash;
            private Url url;

            @ToString
            @Getter
            public static class Data {
                private String value;
                private String signature;
            }

            @ToString
            @Getter
            public static class Hash {
                private String skin;
                private String cape;
            }

            @ToString
            @Getter
            public static class Url {
                private String skin;
                private String cape;
            }
        }

        @ToString
        @Getter
        public static class Generator {
            private String version;
            private long timestamp;
            private long duration;
            private String account;
            private String server;
        }
    }

    @ToString
    @Getter
    public static class RateLimit {
        private Next next;
        private Delay delay;
        private Limit limit;

        @ToString
        @Getter
        public static class Next {
            private long absolute;
            private long relative;
        }

        @ToString
        @Getter
        public static class Delay {
            private long millis;
            private double seconds;
        }

        @ToString
        @Getter
        public static class Limit {
            private int limit;
            private int remaining;
            private long reset;
        }
    }

    @ToString
    @Getter
    public static class Usage {
        private Credits credits;
        private Metered metered;

        @ToString
        @Getter
        public static class Credits {
            private int used;
            private int remaining;
        }

        @ToString
        @Getter
        public static class Metered {
            private int used;
        }
    }

    @ToString
    @Getter
    public static class Error {
        private String code;
        private String message;
    }

    @ToString
    @Getter
    public static class Warning {
        private String code;
        private String message;
    }

    @ToString
    @Getter
    public static class Message {
        private String code;
        private String message;
    }
}
