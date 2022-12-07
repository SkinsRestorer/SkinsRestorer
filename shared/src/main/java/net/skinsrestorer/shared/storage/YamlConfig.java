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
package net.skinsrestorer.shared.storage;

import net.skinsrestorer.axiom.AxiomConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class YamlConfig {
    private final Path file;
    private final AxiomConfiguration config = new AxiomConfiguration();
    private final AxiomConfiguration defaultConfig = new AxiomConfiguration();

    public YamlConfig(Path file) {
        this.file = file;

        Path parent = file.getParent();
        if (!Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadConfig(InputStream is) {
        if (Files.exists(file)) {
            try {
                load();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                defaultConfig.load(is);

                String beforeMerge = config.saveToString();
                config.merge(defaultConfig, true, true, false);

                if (!beforeMerge.equals(config.saveToString())) {
                    config.save(file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                defaultConfig.load(is);
                defaultConfig.save(file);
                load();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Boolean getBoolean(String path) {
        Boolean value = config.getBoolean(path);

        if (value == null) {
            return defaultConfig.getBoolean(path); // Nullable
        } else {
            return value;
        }
    }

    public Boolean getBoolean(String path, Boolean defValue) {
        Boolean value = config.getBoolean(path);
        return value == null ? defValue : value;
    }

    public Integer getInt(String path) {
        Integer value = null;
        try {
            value = config.getInt(path);
        } catch (NumberFormatException ignored) {
            // todo: add logger
        }
        return value == null ? defaultConfig.getInt(path) : value;
    }

    public Integer getInt(String path, Integer defValue) {
        Integer value = null;
        try {
            value = config.getInt(path);
        } catch (NumberFormatException ignored) {
            // todo: add logger
        }
        return value == null ? defValue : value;
    }

    public String getString(String path) {
        String value = config.getString(path);

        if (value == null) {
            return defaultConfig.getString(path); // Nullable
        } else {
            return value;
        }
    }

    public String getString(String path, String defValue) {
        String value = config.getString(path);
        return value == null ? defValue : value;
    }

    public List<String> getStringList(String path) {
        List<String> value = config.getStringList(path);

        if (value == null) {
            return defaultConfig.getStringList(path); // Nullable
        } else {
            return value;
        }
    }

    public List<String> getStringList(String path, String whatToDelete) {
        return getStringList(path).stream()
                .map(str -> str.replace(whatToDelete, "")).collect(Collectors.toList());
    }

    public void load() throws IOException {
        config.load(file);
    }

    public void save() throws IOException {
        config.save(file);
    }

    public void set(String path, Object value) {
        try {
            config.set(path, value);
            save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
