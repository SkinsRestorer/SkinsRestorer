package skinsrestorer.shared.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;

/** Class by Blackfire62 **/

public class ReflectionUtil {

	public static Field getField(Class<?> clazz, String fname) throws Exception {
		Field f = clazz.getField(fname);
		f.setAccessible(true);
		return f;
	}

	public static Field getPrivateField(Class<?> clazz, String fname) throws Exception {
		Field f = clazz.getDeclaredField(fname);
		f.setAccessible(true);
		return f;
	}

	public static Method getMethod(Class<?> clazz, String mname) throws Exception {
		Method m = null;
		try {
			m = clazz.getMethod(mname);
		} catch (Exception e) {
			m = clazz.getDeclaredMethod(mname);
		}
		m.setAccessible(true);
		return m;
	}

	public static Method getMethod(Class<?> clazz, String mname, Class<?>... args) throws Exception {
		Method m = null;
		try {
			m = clazz.getMethod(mname, args);
		} catch (Exception e) {
			m = clazz.getDeclaredMethod(mname, args);
		}
		m.setAccessible(true);
		return m;
	}

	public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... args) throws Exception {
		Constructor<?> c = clazz.getConstructor(args);
		c.setAccessible(true);
		return c;
	}

	public static Enum<?> getEnum(Class<?> clazz, String enumname, String constant) throws Exception {
		Class<?> c = Class.forName(clazz.getName() + "$" + enumname);
		Enum<?>[] econstants = (Enum<?>[]) c.getEnumConstants();
		for (Enum<?> e : econstants) {
			if (e.name().equalsIgnoreCase(constant))
				return e;
		}
		throw new Exception("Enum constant not found " + constant);
	}

	public static Enum<?> getEnum(Class<?> clazz, String constant) throws Exception {
		Class<?> c = Class.forName(clazz.getName());
		Enum<?>[] econstants = (Enum<?>[]) c.getEnumConstants();
		for (Enum<?> e : econstants) {
			if (e.name().equalsIgnoreCase(constant))
				return e;
		}
		throw new Exception("Enum constant not found " + constant);
	}

	public static Class<?> getNMSClass(String clazz) throws Exception {
		return Class.forName("net.minecraft.server." + getServerVersion() + "." + clazz);
	}

	public static Class<?> getBukkitClass(String clazz) throws Exception {
		return Class.forName("org.bukkit.craftbukkit." + getServerVersion() + "." + clazz);
	}

	public static Object invokeMethod(Class<?> clazz, Object obj, String method, Class<?>[] args, Object... initargs)
			throws Exception {
		Object o = getMethod(clazz, method, args).invoke(obj, initargs);
		return o;
	}

	public static Object invokeMethod(Class<?> clazz, Object obj, String method) throws Exception {
		Object o = getMethod(clazz, method).invoke(obj, new Object[] {});
		return o;
	}

	public static Object invokeConstructor(Class<?> clazz, Class<?>[] args, Object... initargs) throws Exception {
		Object o = getConstructor(clazz, args).newInstance(initargs);
		return o;
	}

	public static String getServerVersion() {
		return Bukkit.getServer().getClass().getPackage().getName()
				.substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf('.') + 1);
	}

}
