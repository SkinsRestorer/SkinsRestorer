package skinsrestorer.shared.utils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Scanner;

public class YamlConfig {

	private File file;
	private Object config;

	public YamlConfig(String path, String name) {
		File direc = new File(path);
		if (!direc.exists())
			direc.mkdirs();
		file = new File(path + name + ".yml");
		try {
			file.createNewFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
		reload();
	}

	public YamlConfig(String path, String name, boolean wait) {
		File direc = new File(path);
		if (!direc.exists())
			direc.mkdirs();
		file = new File(path + name + ".yml");
		if (!wait) {
			try {
				file.createNewFile();
			} catch (Exception e) {
				e.printStackTrace();
			}
			reload();
		}
	}

	public void set(String path, Object value) {
		try {
			ReflectionUtil.invokeMethod(config.getClass(), config, "set", new Class<?>[] { String.class, Object.class },
					path, value);
			save();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Object get(String path) {
		try {
			return ReflectionUtil.invokeMethod(config.getClass(), config, "get", new Class<?>[] { String.class }, path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Object get(String path, Object defValue) {
		if (get(path) == null)
			set(path, defValue);

		return get(path);
	}

	public String getString(String path) {
		String s = "";
		try {
			s = get(path).toString();
		} catch (Exception e) {
		}
		return s;
	}

	public String getString(String path, Object defValue) {
		return get(path, defValue).toString();
	}

	public boolean getBoolean(String path) {
		return Boolean.parseBoolean(getString(path));
	}

	public boolean getBoolean(String path, Object defValue) {
		return Boolean.parseBoolean(getString(path, defValue));
	}

	public int getInt(String path) {
		return Integer.parseInt(getString(path));
	}

	public int getInt(String path, Object defValue) {
		return Integer.parseInt(getString(path, defValue));
	}

	@SuppressWarnings("unchecked")
	public List<String> getStringList(String path) {
		try {
			return (List<String>) ReflectionUtil.invokeMethod(config.getClass(), config, "getStringList",
					new Class<?>[] { String.class }, path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void save() {
		try {
			Object provider = ReflectionUtil.invokeMethod(Class.forName("net.md_5.bungee.config.ConfigurationProvider"),
					null, "getProvider", new Class<?>[] { Class.class },
					new Object[] { Class.forName("net.md_5.bungee.config.YamlConfiguration") });

			ReflectionUtil.invokeMethod(provider.getClass(), provider, "save",
					new Class<?>[] { Class.forName("net.md_5.bungee.config.Configuration"), File.class }, config, file);
		} catch (Exception e) {
			try {
				ReflectionUtil.invokeMethod(config.getClass(), config, "save", new Class<?>[] { File.class },
						new Object[] { file });
			} catch (Exception ex) {
				try {
					ReflectionUtil.invokeMethod(config.getClass(), config, "save",
							new Class<?>[] { Class.forName("org.bukkit.configuration.Configuration"), File.class },
							config, file);
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		}
	}

	public void reload() {
		try {
			Object provider = ReflectionUtil.invokeMethod(Class.forName("net.md_5.bungee.config.ConfigurationProvider"),
					null, "getProvider", new Class<?>[] { Class.class },
					new Object[] { Class.forName("net.md_5.bungee.config.YamlConfiguration") });

			config = ReflectionUtil.invokeMethod(provider.getClass(), provider, "load", new Class<?>[] { File.class },
					new Object[] { file });
		} catch (Exception e) {
			try {
				config = ReflectionUtil.invokeMethod(Class.forName("org.bukkit.configuration.file.YamlConfiguration"),
						null, "loadConfiguration", new Class<?>[] { File.class }, new Object[] { file });
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void copyDefaults(InputStream is) {
		if (!file.exists() || isEmpty()) {
			try {
				Files.copy(is, file.toPath());
			} catch (Exception e) {
				try {
					Files.copy(is, file.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public boolean isEmpty() {
		try {
			Scanner input = new Scanner(file);
			if (input.hasNextLine()) {
				input.close();
				return false;
			}
			input.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

}
