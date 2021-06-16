package net.skinsrestorer.shared.serverinfo;

import com.google.common.collect.ImmutableList;

import java.util.List;

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
    MINECRAFT_1_17(755);

    ProtocolEnum(int protocolNumber) {


    }

    static {
        List<String> SUPPORTED_VERSIONS;
        List<Integer> SUPPORTED_VERSION_IDS;

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
                "1.17.x"
        );
        ImmutableList.Builder<Integer> supportedVersionIds = ImmutableList.<Integer>builder().add(
                ProtocolEnum .1_8,
                ProtocolEnum .1_9,
                ProtocolEnum .1_9_1,
                ProtocolEnum .1_9_2,
                ProtocolEnum .1_9_4,
                ProtocolEnum .1_10,
                ProtocolEnum .1_11,
                ProtocolEnum .1_11_1,
                ProtocolEnum .1_12,
                ProtocolEnum .1_12_1,
                ProtocolEnum .1_12_2,
                ProtocolEnum .1_13,
                ProtocolEnum .1_13_1,
                ProtocolEnum .1_13_2,
                ProtocolEnum .1_14,
                ProtocolEnum .1_14_1,
                ProtocolEnum .1_14_2,
                ProtocolEnum .1_14_3,
                ProtocolEnum .1_14_4,
                ProtocolEnum .1_15,
                ProtocolEnum .1_15_1,
                ProtocolEnum .1_15_2,
                ProtocolEnum .1_16,
                ProtocolEnum .1_16_1,
                ProtocolEnum .1_16_2,
                ProtocolEnum .1_16_3,
                ProtocolEnum .1_16_4,
                ProtocolEnum .1_17
        );

        SUPPORTED_VERSIONS = supportedVersions.build();
        SUPPORTED_VERSION_IDS = supportedVersionIds.build();
    }
}
