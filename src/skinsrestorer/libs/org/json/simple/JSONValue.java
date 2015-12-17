package skinsrestorer.libs.org.json.simple;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import skinsrestorer.libs.org.json.simple.parser.JSONParser;
import skinsrestorer.libs.org.json.simple.parser.ParseException;

public class JSONValue {

	public static Object parse(Reader in) {
		try {
			JSONParser parser = new JSONParser();
			return parser.parse(in);
		} catch (Exception e) {
			return null;
		}
	}

	public static Object parse(String s) {
		StringReader in = new StringReader(s);
		return parse(in);
	}

	public static Object parseWithException(Reader in) throws IOException, ParseException {
		JSONParser parser = new JSONParser();
		return parser.parse(in);
	}

	public static Object parseWithException(String s) throws ParseException {
		JSONParser parser = new JSONParser();
		return parser.parse(s);
	}

	public static void writeJSONString(Object value, Writer out) throws IOException {
		if (value == null) {
			out.write("null");
			return;
		}

		if (value instanceof String) {
			out.write('\"');
			out.write(escape((String) value));
			out.write('\"');
			return;
		}

		if (value instanceof Double) {
			if (((Double) value).isInfinite() || ((Double) value).isNaN()) {
				out.write("null");
			} else {
				out.write(value.toString());
			}
			return;
		}

		if (value instanceof Float) {
			if (((Float) value).isInfinite() || ((Float) value).isNaN()) {
				out.write("null");
			} else {
				out.write(value.toString());
			}
			return;
		}

		if (value instanceof Number) {
			out.write(value.toString());
			return;
		}

		if (value instanceof Boolean) {
			out.write(value.toString());
			return;
		}

		if ((value instanceof JSONStreamAware)) {
			((JSONStreamAware) value).writeJSONString(out);
			return;
		}

		if ((value instanceof JSONAware)) {
			out.write(((JSONAware) value).toJSONString());
			return;
		}

		if (value instanceof Map) {
			JSONObject.writeJSONString((Map<?, ?>) value, out);
			return;
		}

		if (value instanceof List) {
			JSONArray.writeJSONString((List<?>) value, out);
			return;
		}

		out.write(value.toString());
	}

	/**
	 * Convert an object to JSON text.
	 * <p>
	 * If this object is a Map or a List, and it's also a JSONAware, JSONAware will be considered firstly.
	 * <p>
	 * DO NOT call this method from toJSONString() of a class that implements both JSONAware and Map or List with "this" as the parameter, use JSONObject.toJSONString(Map) or JSONArray.toJSONString(List) instead.
	 *
	 * @see skinsrestorer.libs.org.json.simple.JSONObject#toJSONString(Map)
	 * @see skinsrestorer.libs.org.json.simple.JSONArray#toJSONString(List)
	 *
	 * @param value
	 * @return JSON text, or "null" if value is null or it's an NaN or an INF number.
	 */
	public static String toJSONString(Object value) {
		if (value == null) {
			return "null";
		}

		if (value instanceof String) {
			return "\"" + escape((String) value) + "\"";
		}

		if (value instanceof Double) {
			if (((Double) value).isInfinite() || ((Double) value).isNaN()) {
				return "null";
			} else {
				return value.toString();
			}
		}

		if (value instanceof Float) {
			if (((Float) value).isInfinite() || ((Float) value).isNaN()) {
				return "null";
			} else {
				return value.toString();
			}
		}

		if (value instanceof Number) {
			return value.toString();
		}

		if (value instanceof Boolean) {
			return value.toString();
		}

		if ((value instanceof JSONAware)) {
			return ((JSONAware) value).toJSONString();
		}

		if (value instanceof Map) {
			return JSONObject.toJSONString((Map<?, ?>) value);
		}

		if (value instanceof List) {
			return JSONArray.toJSONString((List<?>) value);
		}

		return value.toString();
	}

	/**
	 * Escape quotes, \, /, \r, \n, \b, \f, \t and other control characters (U+0000 through U+001F).
	 *
	 * @param s
	 * @return
	 */
	public static String escape(String s) {
		if (s == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer();
		escape(s, sb);
		return sb.toString();
	}

	/**
	 * @param s
	 *            - Must not be null.
	 * @param sb
	 */
	static void escape(String s, StringBuffer sb) {
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			switch (ch) {
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				case '/':
					sb.append("\\/");
					break;
				default:
					// Reference: http://www.unicode.org/versions/Unicode5.1.0/
					if (((ch >= '\u0000') && (ch <= '\u001F')) || ((ch >= '\u007F') && (ch <= '\u009F')) || ((ch >= '\u2000') && (ch <= '\u20FF'))) {
						String ss = Integer.toHexString(ch);
						sb.append("\\u");
						for (int k = 0; k < (4 - ss.length()); k++) {
							sb.append('0');
						}
						sb.append(ss.toUpperCase());
					} else {
						sb.append(ch);
					}
			}
		}// for
	}

}
