package skinsrestorer.libs.org.json.simple;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JSONObject extends HashMap<Object, Object> implements Map<Object, Object>, JSONAware, JSONStreamAware {

	private static final long serialVersionUID = -503443796854799292L;

	public static void writeJSONString(Map<?, ?> map, Writer out) throws IOException {
		if (map == null) {
			out.write("null");
			return;
		}

		boolean first = true;
		Iterator<? extends Entry<?, ?>> iter = map.entrySet().iterator();

		out.write('{');
		while (iter.hasNext()) {
			if (first) {
				first = false;
			} else {
				out.write(',');
			}
			Entry<?, ?> entry = iter.next();
			out.write('\"');
			out.write(escape(String.valueOf(entry.getKey())));
			out.write('\"');
			out.write(':');
			JSONValue.writeJSONString(entry.getValue(), out);
		}
		out.write('}');
	}

	@Override
	public void writeJSONString(Writer out) throws IOException {
		writeJSONString(this, out);
	}

	/**
	 * Convert a map to JSON text. The result is a JSON object. If this map is also a JSONAware, JSONAware specific behaviours will be omitted at this top level.
	 *
	 * @see skinsrestorer.libs.org.json.simple.JSONValue#toJSONString(Object)
	 *
	 * @param map
	 * @return JSON text, or "null" if map is null.
	 */
	public static String toJSONString(Map<?, ?> map) {
		if (map == null) {
			return "null";
		}

		StringBuffer sb = new StringBuffer();
		boolean first = true;
		Iterator<? extends Entry<?, ?>> iter = map.entrySet().iterator();

		sb.append('{');
		while (iter.hasNext()) {
			if (first) {
				first = false;
			} else {
				sb.append(',');
			}

			Entry<?, ?> entry = iter.next();
			toJSONString(String.valueOf(entry.getKey()), entry.getValue(), sb);
		}
		sb.append('}');
		return sb.toString();
	}

	@Override
	public String toJSONString() {
		return toJSONString(this);
	}

	private static String toJSONString(String key, Object value, StringBuffer sb) {
		sb.append('\"');
		if (key == null) {
			sb.append("null");
		} else {
			JSONValue.escape(key, sb);
		}
		sb.append('\"').append(':');

		sb.append(JSONValue.toJSONString(value));

		return sb.toString();
	}

	@Override
	public String toString() {
		return toJSONString();
	}

	public static String toString(String key, Object value) {
		StringBuffer sb = new StringBuffer();
		toJSONString(key, value, sb);
		return sb.toString();
	}

	public static String escape(String s) {
		return JSONValue.escape(s);
	}

}
