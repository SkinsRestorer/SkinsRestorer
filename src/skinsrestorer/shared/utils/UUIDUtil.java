package skinsrestorer.shared.utils;

import java.util.UUID;

public class UUIDUtil {

	public static UUID fromDashlessString(final String input) {
		if (input == null) {
			return null;
		}
		return UUID.fromString(input.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
	}

}
