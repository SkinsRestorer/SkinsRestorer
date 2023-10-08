package net.skinsrestorer.api;

import net.skinsrestorer.api.semver.SemanticVersion;
import net.skinsrestorer.builddata.BuildData;

/**
 * This class is a helper class for third-party plugins to check whether their used API version is compatible with the
 * version of SkinsRestorer installed on the server.
 * <p>
 * If this class is not available, the plugin is using an outdated version of the API. (V14 or below)
 *
 * @since 15.0.0
 */
public class VersionProvider {
    private static final String VERSION_INFO = String.format("SkinsRestorer %s (%s)", BuildData.VERSION, BuildData.COMMIT_SHORT);
    private static final SemanticVersion SEMANTIC_VERSION = SemanticVersion.fromString(BuildData.VERSION);

    /**
     * Checks whether the given API version is compatible with the version of SkinsRestorer installed on the server.
     * If not, the plugin should notify the server admin that the server is using an unsupported version of SkinsRestorer
     * and that the plugin might not work correctly.
     * The version string must be in the format {@code "major"}, {@code "major.minor"} or {@code "major.minor.patch"}.
     *
     * @param version The version to check
     *                (e.g. {@code "15"}, {@code "15.1"} or {@code "15.1.2"})
     * @return {@code true} if the version is compatible, {@code false} otherwise.
     *         The behavior currently is if the major version is different, the API is not compatible.
     *         But this might change in the future.
     */
    public static boolean isCompatibleWith(String version) {
        SemanticVersion semVer = SemanticVersion.fromString(version);
        int[] versionParts = semVer.getVersion();

        if (versionParts.length < 1) {
            throw new IllegalArgumentException("Version must have at least one part");
        }

        return versionParts[0] == SEMANTIC_VERSION.getVersion()[0];
    }

    /**
     * Returns the version of SkinsRestorer installed on the server for a third-party plugin to use for a message.
     * E.g. "Expected SkinsRestorer 16.0.0+, but found [SkinsRestorer 15.0.0 (a1b2c3d)]".
     *
     * @return The version of SkinsRestorer installed on the server.
     *         The format is {@code "SkinsRestorer [version] ([first six letters of commit])"}
     *         but might change in the future.
     *         (e.g. {@code "SkinsRestorer 15.0.0 (a1b2c3d)"})
     */
    public static String getVersionInfo() {
        return VERSION_INFO;
    }

    /**
     * Returns the semantic version of SkinsRestorer installed on the server for a third-party plugin for a third-party
     * plugin to use for detecting the version of SkinsRestorer installed on the server to find the right API implementation
     * to use.
     *
     * @return The semantic version of SkinsRestorer installed on the server.
     *         (e.g. {@code [15, 1, 2]} for {@code "15.1.2"})
     */
    public static int[] getSemanticVersion() {
        return SEMANTIC_VERSION.getVersion();
    }

    /**
     * Returns the version of SkinsRestorer installed on the server.
     *
     * @return The version of SkinsRestorer installed on the server.
     *         (e.g. {@code "15.1.2"})
     */
    public static String getVersion() {
        return BuildData.VERSION;
    }

    /**
     * Returns the 40 letter git commit of SkinsRestorer installed on the server.
     *
     * @return The 40 letter git commit of SkinsRestorer installed on the server.
     */
    public static String getCommit() {
        return BuildData.COMMIT;
    }
}
