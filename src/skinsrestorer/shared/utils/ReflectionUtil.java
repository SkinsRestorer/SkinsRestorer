package skinsrestorer.shared.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.bukkit.Bukkit;

/** Class by Blackfire62 **/

public class ReflectionUtil {

	public static final String serverVersion = null;

	static {
		try {
			Class.forName("org.bukkit.Bukkit");
			setObject(ReflectionUtil.class, null, "serverVersion", Bukkit.getServer().getClass().getPackage().getName()
					.substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf('.') + 1));
		} catch (Exception e) {
		}
	}

	public static Field getField(Class<?> clazz, String fname) throws Exception {
		Field f = null;
		try {
			f = clazz.getDeclaredField(fname);
		} catch (Exception e) {
			f = clazz.getField(fname);
		}
		f.setAccessible(true);
		Field modifiers = Field.class.getDeclaredField("modifiers");
		modifiers.setAccessible(true);
		modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
		return f;
	}

	public static Object getObject(Object obj, String fname) throws Exception {
		return getField(obj.getClass(), fname).get(obj);
	}

	public static Object getObject(Class<?> clazz, Object obj, String fname) throws Exception {
		return getField(clazz, fname).get(obj);
	}

	public static void setObject(Object obj, String fname, Object value) throws Exception {
		getField(obj.getClass(), fname).set(obj, value);
	}

	public static void setObject(Class<?> clazz, Object obj, String fname, Object value) throws Exception {
		getField(clazz, fname).set(obj, value);
	}

	public static Method getMethod(Class<?> clazz, String mname) throws Exception {
		Method m = null;
		try {
			m = clazz.getDeclaredMethod(mname);
		} catch (Exception e) {
			try {
				m = clazz.getMethod(mname);
			} catch (Exception ex) {
				for (Method me : clazz.getDeclaredMethods()) {
					if (me.getName().equalsIgnoreCase(mname))
						m = me;
					break;
				}
				if (m == null)
					for (Method me : clazz.getMethods()) {
						if (me.getName().equalsIgnoreCase(mname))
							m = me;
						break;
					}
			}
		}
		m.setAccessible(true);
		return m;
	}

	public static Method getMethod(Class<?> clazz, String mname, Class<?>... args) throws Exception {
		Method m = null;
		try {
			m = clazz.getDeclaredMethod(mname, args);
		} catch (Exception e) {
			try {
				m = clazz.getMethod(mname, args);
			} catch (Exception ex) {
				for (Method me : clazz.getDeclaredMethods()) {
					if (me.getName().equalsIgnoreCase(mname))
						m = me;
					break;
				}
				if (m == null)
					for (Method me : clazz.getMethods()) {
						if (me.getName().equalsIgnoreCase(mname))
							m = me;
						break;
					}
			}
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
		return Class.forName("net.minecraft.server." + serverVersion + "." + clazz);
	}

	public static Class<?> getBukkitClass(String clazz) throws Exception {
		return Class.forName("org.bukkit.craftbukkit." + serverVersion + "." + clazz);
	}

	public static Object invokeMethod(Class<?> clazz, Object obj, String method, Class<?>[] args, Object... initargs)
			throws Exception {
		return getMethod(clazz, method, args).invoke(obj, initargs);
	}

	public static Object invokeMethod(Class<?> clazz, Object obj, String method) throws Exception {
		return getMethod(clazz, method).invoke(obj, new Object[] {});
	}

	public static Object invokeMethod(Class<?> clazz, Object obj, String method, Object... initargs) throws Exception {
		return getMethod(clazz, method).invoke(obj, initargs);
	}

	public static Object invokeMethod(Object obj, String method) throws Exception {
		return getMethod(obj.getClass(), method).invoke(obj, new Object[] {});
	}

	public static Object invokeMethod(Object obj, String method, Object[] initargs) throws Exception {
		return getMethod(obj.getClass(), method).invoke(obj, initargs);
	}

	public static Object invokeConstructor(Class<?> clazz, Class<?>[] args, Object... initargs) throws Exception {
		return getConstructor(clazz, args).newInstance(initargs);
	}

}
