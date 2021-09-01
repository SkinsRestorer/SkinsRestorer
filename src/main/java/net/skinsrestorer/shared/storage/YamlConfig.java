/*
 * SkinsRestorer
 *
 * Copyright (C) 2021 SkinsRestorer
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

import net.skinsrestorer.shared.utils.log.SRLogger;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YamlConfig {
    private final String name;
    private final boolean setMissing;
    private final File file;
    private final SRLogger logger;
    private ConfigurationNode config;

    public YamlConfig(File path, String name, boolean setMissing, SRLogger logger) {
        this.name = name;
        this.setMissing = setMissing;
        this.file = new File(path, name);
        this.logger = logger;

        if (!path.exists())
            //noinspection ResultOfMethodCallIgnored
            path.mkdirs();
    }

    public void saveDefaultConfig(InputStream is) {
        if (file.exists())
            return;

        // create empty file if we got no InputStream with default config
        if (is == null) {
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            Files.copy(is, file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            reload();
        } catch (ConfigurateException e) {
            e.printStackTrace();
        }
    }

    public ConfigurationNode get(String path) {
        try {
            return config.node((Object[]) path.split("\\."));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public ConfigurationNode get(String path, String defValue) {
        // Save new values if enabled (locale file)
        if (get(path).virtual() && setMissing) {
            logger.info("Saving new config value " + path + " to " + name);
            set(path, defValue);
        }

        return get(path);
    }

    public boolean getBoolean(String path) {
        return get(path).getBoolean();
    }

    public boolean getBoolean(String path, Boolean defValue) {
        return get(path).getBoolean(defValue);
    }

    public int getInt(String path) {
        return get(path).getInt();
    }

    public int getInt(String path, Integer defValue) {
        return get(path).getInt(defValue);
    }

    private String getString(String path) {
        return get(path).getString();
    }

    public String getString(String path, String defValue) {
        return get(path, defValue).getString(defValue);
    }

    public List<String> getStringList(String path) {
        try {
            return get(path).getList(String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public List<String> getStringList(String path, String whatToDelete) {
        try {
            List<String> list = getStringList(path);
            List<String> newList = new ArrayList<>();

            for (String str : list)
                newList.add(str.replace(whatToDelete, ""));

            return newList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public void reload() throws ConfigurateException {
        config = YamlConfigurationLoader.builder().path(file.toPath()).build().load();
    }

    private void save() throws ConfigurateException {
        YamlConfigurationLoader.builder().path(file.toPath()).build().save(config);
    }

    public void set(String path, Object value) {
        try {
            ConfigurationNode node = config.node((Object[]) path.split("\\."));
            if (value instanceof List) {
                //noinspection unchecked
                node.setList(String.class, (List<String>) value);
            } else {
                node.set(value);
            }

            save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
