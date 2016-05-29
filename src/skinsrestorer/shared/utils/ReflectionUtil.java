package skinsrestorer.shared.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;

public class ReflectionUtil {

	public static Field getField(Class<?> clazz, String fname) {
		try {
			Field f = clazz.getField(fname);
			f.setAccessible(true);
			return f;
		} catch (Exception e) {

			System.out.println(clazz.getName() + fname);

			return null;
		}
	}

	public static Field getPrivateField(Class<?> clazz, String fname) {
		try {
			Field f = clazz.getDeclaredField(fname);
			f.setAccessible(true);
			return f;
		} catch (Exception e) {

			System.out.println(clazz.getName() + fname);

			return null;
		}
	}

	public static Method getMethod(Class<?> clazz, String mname) {
		try {
			Method m = clazz.getMethod(mname);
			m.setAccessible(true);
			return m;
		} catch (Exception e) {

			System.out.println(clazz.getName() + mname);

			return null;
		}
	}

	public static Method getMethod(Class<?> clazz, String mname, Class<?>... args) {
		try {
			Method m = clazz.getMethod(mname, args);
			m.setAccessible(true);
			return m;
		} catch (Exception e) {

			System.out.println(clazz.getName() + args);

			return null;
		}
	}

	public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... args) {
		try {
			Constructor<?> c = clazz.getConstructor(args);
			c.setAccessible(true);
			return c;
		} catch (Exception e) {

			System.out.println(clazz.getName() + args);

			return null;
		}
	}

	public static Enum<?> getEnum(Class<?> clazz, String enumname, String constant) {
		try {
			Class<?> c = Class.forName(clazz.getName() + "$" + enumname);
			Enum<?>[] econstants = (Enum<?>[]) c.getEnumConstants();
			for (Enum<?> e : econstants) {
				if (e.name().equalsIgnoreCase(constant))
					return e;
			}
			throw new Exception("Enum constant not found " + constant);
		} catch (Exception e) {

			System.out.println(clazz.getName() + "$" + enumname);

			return null;
		}
	}

	public static Enum<?> getEnum(Class<?> clazz, String constant) {
		try {
			Class<?> c = Class.forName(clazz.getName());
			Enum<?>[] econstants = (Enum<?>[]) c.getEnumConstants();
			for (Enum<?> e : econstants) {
				if (e.name().equalsIgnoreCase(constant))
					return e;
			}
			throw new Exception("Enum constant not found " + constant);
		} catch (Exception e) {

			System.out.println(clazz.getName());

			return null;
		}
	}

	public static Class<?> getNMSClass(String clazz) {
		try {
			return Class.forName("net.minecraft.server." + getServerVersion() + "." + clazz);
		} catch (ClassNotFoundException e) {

			System.out.println("net.minecraft.server." + getServerVersion() + "." + clazz);

			return null;
		}
	}

	public static Class<?> getBukkitClass(String clazz) {
		try {
			return Class.forName("org.bukkit.craftbukkit." + getServerVersion() + "." + clazz);
		} catch (ClassNotFoundException e) {

			System.out.println("org.bukkit.craftbukkit." + getServerVersion() + "." + clazz);

			return null;
		}
	}

	public static Object invokeMethod(Class<?> clazz, Object obj, String method, Class<?>[] args, Object... initargs) {
		try {
			Object o = getMethod(clazz, method, args).invoke(obj, initargs);
			return o;
		} catch (Exception e) {

			return null;
		}
	}

	public static Object invokeMethod(Class<?> clazz, Object obj, String method) {
		try {
			Object o = getMethod(clazz, method).invoke(obj, new Object[] {});
			return o;
		} catch (Exception e) {

			return null;
		}
	}

	public static Object invokeConstructor(Class<?> clazz, Class<?>[] args, Object... initargs) {
		try {
			Object o = getConstructor(clazz, args).newInstance(initargs);
			return o;
		} catch (Exception e) {
			return null;
		}
	}

	public static String getServerVersion() {
		return Bukkit.getServer().getClass().getPackage().getName()
				.substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf('.') + 1);
	}

}
