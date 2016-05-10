package skinsrestorer.shared.storage;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import skinsrestorer.shared.utils.DataFiles;

public class LocaleStorage {

	public String PLAYER_SKIN_CHANGE_SKIN_DATA_CLEARED = null;
	public String PLAYER_SKIN_COOLDOWN = null;
	public String PLAYER_SKIN_CHANGE_SUCCESS = null;
	public String PLAYER_SKIN_CHANGE_SUCCESS_DATABASE = null;
	public String DO_YOU_WANT_SKIN = null;

	public String PLAYER_HAS_NO_PERMISSION = null;
	public String DISABLED_SKIN = null;
	public String UNKNOWN_COMMAND = null;

	public String SKIN_FETCH_FAILED_NO_PREMIUM_PLAYER = null;
	public String SKIN_FETCH_FAILED_NO_SKIN_DATA = null;
	public String SKIN_FETCH_FAILED_PARSE_FAILED = null;
	public String SKIN_FETCH_FAILED_RATE_LIMITED = null;
	public String SKIN_FETCH_FAILED_MCAPI_PROBLEM = null;
	public String SKIN_FETCH_FAILED_ERROR = null;
	public String SKIN_DATA_DROPPED = null;
	public String SKIN_DATA_SAVED = null;
	public String SKIN_DATA_UPDATED = null;
	public String SKIN_FETCH_FAILED = null;
	public String MCAPI_FAILED_ERROR = null;
	public String TRYING_TO_USE_NCAPI = null;

	public String USE_SKIN_HELP = null;
	public List<String> PLAYER_HELP = new ArrayList<String>();
	public String ADMIN_USE_SKIN_HELP = null;
	public String ADMIN_SET_SKIN = null;
	public List<String> ADMIN_HELP = new ArrayList<String>();

	private static final LocaleStorage instance = new LocaleStorage();
	private static DataFiles locale = new DataFiles("plugins" + File.separator + "SkinsRestorer" + File.separator + "",
			"messages");

	public static final LocaleStorage getInstance() {
		return instance;
	}

	public void init(InputStream stream, boolean overWrite) {
		locale.copyDefaults(stream, overWrite);
		locale.reload();
		PLAYER_SKIN_CHANGE_SKIN_DATA_CLEARED = locale.getString("Messages.PLAYER_SKIN_CHANGE_SKIN_DATA_CLEARED");
		PLAYER_SKIN_COOLDOWN = locale.getString("Messages.PLAYER_SKIN_COOLDOWN");
		PLAYER_SKIN_CHANGE_SUCCESS = locale.getString("Messages.PLAYER_SKIN_CHANGE_SUCCESS");
		PLAYER_SKIN_CHANGE_SUCCESS_DATABASE = locale.getString("Messages.PLAYER_SKIN_CHANGE_SUCCESS_DATABASE");
		DO_YOU_WANT_SKIN = locale.getString("Messages.DO_YOU_WANT_SKIN");

		PLAYER_HAS_NO_PERMISSION = locale.getString("Messages.PLAYER_HAS_NO_PERMISSION");
		DISABLED_SKIN = locale.getString("Messages.DISABLED_SKIN");
		UNKNOWN_COMMAND = locale.getString("Messages.UNKNOWN_COMMAND");

		SKIN_FETCH_FAILED_NO_PREMIUM_PLAYER = locale.getString("Messages.SKIN_FETCH_FAILED_NO_PREMIUM_PLAYER");
		SKIN_FETCH_FAILED_NO_SKIN_DATA = locale.getString("Messages.SKIN_FETCH_FAILED_NO_SKIN_DATA");
		SKIN_FETCH_FAILED_PARSE_FAILED = locale.getString("Messages.SKIN_FETCH_FAILED_PARSE_FAILED");
		SKIN_FETCH_FAILED_RATE_LIMITED = locale.getString("Messages.SKIN_FETCH_FAILED_RATE_LIMITED");
		SKIN_FETCH_FAILED_MCAPI_PROBLEM = locale.getString("Messages.SKIN_FETCH_FAILED_MCAPI_PROBLEM");
		SKIN_FETCH_FAILED_ERROR = locale.getString("Messages.SKIN_FETCH_FAILED_ERROR");
		SKIN_DATA_DROPPED = locale.getString("Messages.SKIN_DATA_DROPPED");
		SKIN_DATA_SAVED = locale.getString("Messages.SKIN_DATA_SAVED");
		SKIN_DATA_UPDATED = locale.getString("Messages.SKIN_DATA_UPDATED");
		SKIN_FETCH_FAILED = locale.getString("Messages.SKIN_FETCH_FAILED");
		MCAPI_FAILED_ERROR = locale.getString("Messages.MCAPI_FAILED_ERROR");
		TRYING_TO_USE_NCAPI = locale.getString("Messages.TRYING_TO_USE_NCAPI");

		USE_SKIN_HELP = locale.getString("Messages.USE_SKIN_HELP");
		PLAYER_HELP = locale.getStringList("Messages.PLAYER_HELP");

		ADMIN_USE_SKIN_HELP = locale.getString("Messages.ADMIN_USE_SKIN_HELP");
		ADMIN_SET_SKIN = locale.getString("Messages.ADMIN_SET_SKIN");
		ADMIN_HELP = locale.getStringList("Messages.ADMIN_HELP");
		locale.save();
	}
}