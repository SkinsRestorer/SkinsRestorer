/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
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
 * #L%
 */
package net.skinsrestorer.shared.storage;

import net.skinsrestorer.shared.utils.ReflectionUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class YamlConfig {
    private final String path;
    private final String name;
    private final File file;

    private Object config;
    private final boolean setMissing;

    public YamlConfig(String path, String name, boolean setMissing) {
        File direc = new File(path);
        if (!direc.exists())
            direc.mkdirs();

        this.path = path;
        this.name = name + ".yml";
        this.setMissing = setMissing;
        this.file = new File(this.path + this.name);
    }

    private void createNewFile() {
        try {
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveDefaultConfig() {
        this.saveDefaultConfig(null);
    }

    public void saveDefaultConfig(InputStream is) {
        if (file.exists())
            return;

        // create empty file if we got no InputStream with default config
        if (is == null) {
            this.createNewFile();
            return;
        }

        this.saveResource(is, this.name, false);
        this.reload();
    }

    public void saveResource(InputStream in, String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in ");
        }

        File outFile = new File(path, resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(path, resourcePath.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                try (OutputStream out = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
                in.close();
            } else {
                System.out.println("Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            System.out.println("Could not save " + outFile.getName() + " to " + outFile);
            ex.printStackTrace();
        }
    }

    public Object get(String path) {
        try {
            return ReflectionUtil.invokeMethod(config.getClass(), config, "get", new Class<?>[]{String.class}, path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object get(String path, Object defValue) {
        if (get(path) == null && !this.setMissing) {
            System.out.println("[SkinsRestorer] " + path + " is missing in " + this.name + "! Using default value.");
            return defValue;
        }

        // Save new values if enabled (locale file)
        if (get(path) == null && this.setMissing) {
            System.out.println("[SkinsRestorer] Saving new config value " + path + " to " + this.name);
            set(path, defValue);
        }

        return get(path);
    }

    public boolean getBoolean(String path) {
        return Boolean.parseBoolean(getString(path));
    }

    public boolean getBoolean(String path, Boolean defValue) {
        return Boolean.parseBoolean(getString(path, defValue));
    }

    public File getFile() {
        return file;
    }

    public int getInt(String path) {
        return Integer.parseInt(getString(path));
    }

    public int getInt(String path, Integer defValue) {
        return Integer.parseInt(getString(path, defValue));
    }

    private String getString(String path) {
        String s;
        s = get(path).toString();
        return s;
    }

    public String getString(String path, Object defValue) {
        return get(path, defValue).toString();
    }

    public List<String> getStringList(String path) {
        try {
            return (List<String>) ReflectionUtil.invokeMethod(config.getClass(), config, "getStringList",
                    new Class<?>[]{String.class}, path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getStringList(String path, String whatToDelete) {
        try {
            List<String> list = (List<String>) ReflectionUtil.invokeMethod(config.getClass(), config, "getStringList",
                    new Class<?>[]{String.class}, path);
            List<String> newList = new ArrayList<>();

            for (String str : list)
                newList.add(str.replace(whatToDelete, ""));

            return newList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isEmpty() {
        try {
            try (Scanner input = new Scanner(file)) {
                if (input.hasNextLine())
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public void reload() {
        try {
            Object provider = ReflectionUtil.invokeMethod(Class.forName("net.md_5.bungee.config.ConfigurationProvider"),
                    null, "getProvider", new Class<?>[]{Class.class},
                    Class.forName("net.md_5.bungee.config.YamlConfiguration"));

            config = ReflectionUtil.invokeMethod(provider.getClass(), provider, "load", new Class<?>[]{File.class}, file);
        } catch (Exception e) {
            try {
                config = ReflectionUtil.invokeMethod(Class.forName("org.bukkit.configuration.file.YamlConfiguration"),
                        null, "loadConfiguration", new Class<?>[]{File.class}, file);
            } catch (Exception ex) {
                e.printStackTrace();
            }
        }
    }

    private void save() {
        try {
            Object provider = ReflectionUtil.invokeMethod(Class.forName("net.md_5.bungee.config.ConfigurationProvider"),
                    null, "getProvider", new Class<?>[]{Class.class},
                    Class.forName("net.md_5.bungee.config.YamlConfiguration"));

            ReflectionUtil.invokeMethod(provider.getClass(), provider, "save",
                    new Class<?>[]{Class.forName("net.md_5.bungee.config.Configuration"), File.class}, config, file);
        } catch (Exception e) {
            try {
                ReflectionUtil.invokeMethod(config.getClass(), config, "save", new Class<?>[]{File.class}, file);
            } catch (Exception ex) {
                try {
                    ReflectionUtil.invokeMethod(config.getClass(), config, "save",
                            new Class<?>[]{Class.forName("org.bukkit.configuration.Configuration"), File.class},
                            config, file);
                } catch (Exception exc) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void set(String path, Object value) {
        try {
            ReflectionUtil.invokeMethod(config.getClass(), config, "set", new Class<?>[]{String.class, Object.class},
                    path, value);
            save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
