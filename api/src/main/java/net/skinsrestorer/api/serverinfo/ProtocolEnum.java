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
package net.skinsrestorer.api.serverinfo;

import com.google.common.collect.ImmutableList;

import java.util.List;

@SuppressWarnings("unused")
public enum ProtocolEnum {

    MINECRAFT_1_8(47),
    MINECRAFT_1_9(107),
    MINECRAFT_1_9_1(108),
    MINECRAFT_1_9_2(109),
    MINECRAFT_1_9_4(110),
    MINECRAFT_1_10(210),
    MINECRAFT_1_11(315),
    MINECRAFT_1_11_1(316),
    MINECRAFT_1_12(335),
    MINECRAFT_1_12_1(338),
    MINECRAFT_1_12_2(340),
    MINECRAFT_1_13(393),
    MINECRAFT_1_13_1(401),
    MINECRAFT_1_13_2(404),
    MINECRAFT_1_14(477),
    MINECRAFT_1_14_1(480),
    MINECRAFT_1_14_2(485),
    MINECRAFT_1_14_3(490),
    MINECRAFT_1_14_4(498),
    MINECRAFT_1_15(573),
    MINECRAFT_1_15_1(575),
    MINECRAFT_1_15_2(578),
    MINECRAFT_1_16(735),
    MINECRAFT_1_16_1(736),
    MINECRAFT_1_16_2(751),
    MINECRAFT_1_16_3(753),
    MINECRAFT_1_16_4(754),
    MINECRAFT_1_17(755),
    MINECRAFT_1_17_1(756),
    MINECRAFT_1_18(757);

    static {
        List<String> SUPPORTED_VERSIONS;

        ImmutableList.Builder<String> supportedVersions = ImmutableList.<String>builder().add(
                "1.8.x",
                "1.9.x",
                "1.10.x",
                "1.11.x",
                "1.12.x",
                "1.13.x",
                "1.14.x",
                "1.15.x",
                "1.16.x",
                "1.17.x",
                "1.18.x"
        );

        SUPPORTED_VERSIONS = supportedVersions.build();
    }

    ProtocolEnum(int protocolNumber) {
    }
}
