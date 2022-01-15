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
import net.skinsrestorer.shared.exception.YamlException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class YamlConfig {
    private final File file;
    private final AxiomConfiguration config = new AxiomConfiguration();

    public YamlConfig(File path, String name) {
        this.file = new File(path, name);

        if (!path.exists())
            //noinspection ResultOfMethodCallIgnored
            path.mkdirs();
    }

    public void saveDefaultConfig(InputStream is) {
        if (file.exists() && is != null) {
            try {
                AxiomConfiguration defaultConfig = new AxiomConfiguration();
                defaultConfig.load(is);
                config.mergeDefault(defaultConfig, true, false);
                config.save(file.toPath());
            } catch(IOException e) {
                e.printStackTrace();
            }
        } else {
            // create empty file if we got no InputStream with default config
            if (is == null) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Files.copy(is, file.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            reload();
        } catch (YamlException e) {
            e.printStackTrace();
        }
    }

    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    public boolean getBoolean(String path, Boolean defValue) {
        Boolean value = config.getBoolean(path);
        return value == null ? defValue : value;
    }

    public int getInt(String path) {
        return config.getInt(path);
    }

    public int getInt(String path, Integer defValue) {
        Integer value = config.getInt(path);
        return value == null ? defValue : value;
    }

    private String getString(String path) {
        return config.getString(path);
    }

    public String getString(String path, String defValue) {
        String value = config.getString(path);
        return value == null ? defValue : value;
    }

    public List<String> getStringList(String path) {
        try {
            return config.getStringList(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public List<String> getStringList(String path, String whatToDelete) {
        return config.getStringList(path).stream()
                .map(str -> str.replace(whatToDelete, "")).collect(Collectors.toList());
    }

    public void reload() throws YamlException {
        try {
            config.load(file.toPath());
        } catch (IOException ex) {
            throw new YamlException(ex);
        }
    }

    private void save() throws YamlException {
        try {
            config.save(file.toPath());
        } catch (IOException ex) {
            throw new YamlException(ex);
        }
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
