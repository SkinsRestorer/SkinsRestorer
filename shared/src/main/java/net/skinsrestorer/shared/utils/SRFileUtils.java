/*
 * SkinsRestorer
 *
 * Copyright (C) 2023 SkinsRestorer
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
package net.skinsrestorer.shared.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SRFileUtils {
    public static void renameFile(Path parent, String oldName, String newName) throws IOException {
        try (Stream<Path> stream = Files.list(parent)) {
            // Folders are case-insensitive on Windows, so we need to check it using this method
            List<String> files = stream.map(Path::getFileName).map(Path::toString).collect(Collectors.toList());

            String tempName = newName + "_temp";
            if (files.contains(oldName) && !files.contains(tempName) && !files.contains(newName)) {
                Path oldPath = parent.resolve(oldName);
                Path tempPath = parent.resolve(tempName);
                Path newPath = parent.resolve(newName);

                // Windows will not allow renaming a folder to a name that differs only in case
                // So we need to rename it to a temporary name first
                Files.move(oldPath, tempPath, StandardCopyOption.REPLACE_EXISTING);
                Files.move(tempPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
