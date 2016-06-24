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
		String s = null;
		try {
			s = get(path).toString();
		} catch (Exception e) {
		}
		return s;
	}

	public boolean getBoolean(String path) {
		return Boolean.parseBoolean(getString(path));
	}

	public int getInt(String path) {
		return Integer.parseInt(getString(path));
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
			ReflectionUtil.invokeMethod(config.getClass(), config, "save", new Class<?>[] { File.class }, file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void reload() {
		try {
			Object provider = ReflectionUtil.invokeMethod(Class.forName("net.md_5.bungee.config.ConfigurationProvider"),
					null, "getProvider", new Class<?>[] { Class.class },
					Class.forName("net.md_5.bungee.config.YamlConfiguration"));

			config = ReflectionUtil.invokeMethod(provider.getClass(), provider, "load", new Class<?>[] { File.class },
					file);
		} catch (Exception e) {
			try {
				config = ReflectionUtil.invokeMethod(Class.forName("org.bukkit.configuration.file.YamlConfiguration"),
						null, "loadConfiguration", new Class<?>[] { File.class }, file);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void copyDefaults(InputStream is, boolean overWrite) {
		if (overWrite || !file.exists() || isEmpty()) {
			if (is == null) {
				System.out.println("[Warning] " + file.getName() + "'s .jar file have been modified!");
				System.out.println("[Warning] Could not generate " + file.getName() + "!");
				System.out.println("[Warning] Please stop and restart the server completely!");
				return;
			}
			try {
				Files.copy(is, file.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				e.printStackTrace();
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
