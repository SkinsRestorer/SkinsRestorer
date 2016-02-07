package skinsrestorer.shared.storage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;

import skinsrestorer.libs.com.google.gson.Gson;
import skinsrestorer.libs.com.google.gson.GsonBuilder;

public class LocaleStorage {

	private static final String localefile = "locale.json";
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	private static final LocaleStorage instance = new LocaleStorage();

	public static final LocaleStorage getInstance() {
		return instance;
	}

	public static void init(File datafolder) {
		File fullpath = new File(datafolder, localefile);
		try (FileReader reader = new FileReader(fullpath)) {
			LocaleStorage other = gson.fromJson(reader, LocaleStorage.class);
			for (Field field : LocaleStorage.class.getFields()) {
				Object value = field.get(other);
				if (value != null) {
					field.set(instance, value);
				}
			}
		} catch (IOException | IllegalArgumentException | IllegalAccessException e) {
		}
		try (FileWriter writer = new FileWriter(fullpath)) {
			datafolder.mkdirs();
			writer.write(gson.toJson(instance));
		} catch (IOException e) {
		}
	}

	public String PLAYER_SKIN_CHANGE_SKIN_DATA_CLEARED = "Your skin data has been removed";
	public String PLAYER_SKIN_CHANGE_COOLDOWN = "You can change your skin only once per 10 minutes";
	public String PLAYER_SKIN_CHANGE_SUCCESS = "Your skin has been updated.";
	public String PLAYER_SKIN_CHANGE_FAILED = "Skin fetch failed: ";
	
	public String PLAYER_HAS_NO_PERMISSION = "You don't have permission to do this!";

	public String SKIN_FETCH_FAILED_NO_PREMIUM_PLAYER = "Can't find a valid premium player with that name";
	public String SKIN_FETCH_FAILED_NO_SKIN_DATA = "No skin data found for player with that name";
	public String SKIN_FETCH_FAILED_PARSE_FAILED = "Can't decode skin data";
	public String SKIN_FETCH_FAILED_RATE_LIMITED = "Rate limited";
	public String SKIN_FETCH_FAILED_ERROR = "An error has occured";
	public String SKIN_DATA_DROPPED = "Skin data for player %player dropped";
	public String SKIN_DATA_SAVED = "Skin data saved successfully.";
	public String SKIN_DATA_UPDATED = "Skin data updated";
	public String SKIN_FETCH_FAILED = "Skin fetch failed: ";
	public String MCAPI_FAILED_ERROR = "Tried to use mcapi.ca, but it failed.";
	public String TRYING_TO_USE_NCAPI = "[SkinsRestorer] Getting skin from Mojang failed. Using mcapi.ca!";
	
	public String USE_SKIN_HELP = "Use '/skin help' for help.";
	public String PLAYER_HELP = "&8]&7&m-------------&r&8[ &9SkinsRestorer Help &8]&7&m-------------*r&8[\n&9/skin set <skinname> &9-&a Sets your skin.\n&9/skin clear &9-&a Clears your skin.";
	
	public String ADMIN_USE_SKIN_HELP = "&9Use '/skinsrestorer help' for help.";
	public String ADMIN_SET_SKIN = "You set %player''s skin.";
	public String ADMIN_HELP = "&8]&7&m-------------&r&8[ &9SkinsRestorer Admin Help &8]&7&m-------------*r&8[\n&9/skinsrestorer drop <player> &9-&a Drops player skin data.\n&9/skinsrestorer update <player> &9-&a Updates player skin data.\n&9/skinsrestorer set <player> <skin name> &9-&a Sets Player's skin.";

}
