/*
 * Copyright 2016 inventivetalent. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  The views and conclusions contained in the software and documentation are those of the
 *  authors and contributors and should not be interpreted as representing official policies,
 *  either expressed or implied, of anybody else.
 */

package skinsrestorer.shared.utils;

public abstract class VersionComparator {

    /**
     * Compares versions by checking if the version strings are equal
     */
    public static final VersionComparator EQUAL = new VersionComparator() {
        @Override
        public boolean isNewer(String currentVersion, String checkVersion) {
            return !currentVersion.equals(checkVersion);
        }
    };

    /**
     * Compares versions by their Sematic Version (<code>Major.Minor.Patch</code>, <a href="http://semver.org/">semver.org</a>). Removes dots and compares the resulting Integer values
     */
    public static final VersionComparator SEM_VER = new VersionComparator() {
        @Override
        public boolean isNewer(String currentVersion, String checkVersion) {
            currentVersion = currentVersion.replace(".", "");
            checkVersion = checkVersion.replace(".", "");

            try {
                int current = Integer.parseInt(currentVersion);
                int check = Integer.parseInt(checkVersion);

                return check > current;
            } catch (NumberFormatException e) {
                System.err.println("[SpigetUpdate] Invalid SemVer versions specified [" + currentVersion + "] [" + checkVersion + "]");
            }
            return false;
        }
    };

    /**
     * Same as {@link VersionComparator#SEM_VER}, but supports version names with '-SNAPSHOT' prefixes
     */
    public static final VersionComparator SEM_VER_SNAPSHOT = new VersionComparator() {
        @Override
        public boolean isNewer(String currentVersion, String checkVersion) {
            currentVersion = currentVersion.replace("-BETA", "");
            checkVersion = checkVersion.replace("-BETA", "");

            return SEM_VER.isNewer(currentVersion, checkVersion);
        }
    };

    /**
     * Called to check if a version is newer
     *
     * @param currentVersion Current version of the plugin
     * @param checkVersion   Version to check
     * @return <code>true</code> if the checked version is newer
     */
    public abstract boolean isNewer(String currentVersion, String checkVersion);

}

