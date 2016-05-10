package skinsrestorer.shared.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DataFiles {
	private File file;

	public DataFiles(String name) {
		this("", name);
	}

	public DataFiles(String path, String name) {
		this(path, name, ".yml");
	}

	public DataFiles(String path, String name, String type) {
		File direc = new File(path);
		if (!direc.exists())
			direc.mkdirs();
		file = new File(path + name + type);
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		reload();
	}

	public DataFiles(File file) {
		try {
			file.createNewFile();
			this.file = file;
		} catch (IOException e) {
			e.printStackTrace();
		}
		reload();
	}

	private Map<Integer, String> sec = new HashMap<Integer, String>();
	private Map<Integer, String> content = new HashMap<Integer, String>();
	private List<Object> listings = new ArrayList<Object>();

	public void set(String path, Object value) {
		int counter = 1;
		int lineNumber = 1;
		int mark = 1;
		section(counter, path);
		while (true) {
			String line = content.get(lineNumber);
			if (line == null || !isCommented(line)) {
				if (line != null && !isCommented(line) && line.startsWith(tab(counter))) {
					mark = lineNumber;
				}
				if (line == null || (!isCommented(line) && !line.startsWith(tab(counter)))) {
					lineNumber = mark;
					if (content.get(lineNumber) == null)
						lineNumber--;
					if (counter <= sec.size()) {
						lineNumber++;
						for (int x = content.size(); x >= lineNumber; x--) {
							content.put(x + sec.size() - counter + 1, content.get(x));
							content.remove(content.get(x));
						}
						while (counter < sec.size()) {
							content.put(lineNumber, sec.get(counter));
							counter++;
							lineNumber++;
						}
					}
					counter = sec.size();
					if (value instanceof List) {
						setList(lineNumber, counter, mark, (List<?>) value);
						return;
					} else {
						content.put(lineNumber, sec.get(counter) + " " + value);
						return;
					}
				}
				if (line.startsWith(sec.get(counter))) {
					counter++;
				}
			}
			lineNumber++;
		}
	}

	private void setList(int lineNumber, int counter, int mark, List<?> value) {
		if (!content.get(lineNumber).startsWith(sec.get(counter)))
			content.put(lineNumber, sec.get(counter));
		mark = lineNumber;
		int last = lineNumber;
		lineNumber++;
		while (content.get(lineNumber) != null
				&& (isCommented(content.get(lineNumber)) || isListing(content.get(lineNumber)))) {
			if (isListing(content.get(lineNumber)))
				last = lineNumber;
			lineNumber++;
		}
		int diff = ((List<?>) value).size() - (last - mark);
		lineNumber = last + 1;
		if (diff > 0) {
			for (int ind = content.size(); ind >= lineNumber; ind--) {
				content.put(ind + diff, content.get(ind));
			}
		} else if (diff < 0) {
			while (content.get(lineNumber) != null) {
				content.put(lineNumber + diff, content.get(lineNumber));
				content.remove(lineNumber);
				lineNumber++;
			}
		}
		for (Object o : value) {
			content.put(mark + 1, tab(counter) + "- " + o);
			mark++;
		}

	}

	public Object get(String path) {
		String value = null;
		int counter = 1;
		int lineNumber = 1;
		section(counter, path);
		while (content.get(lineNumber) != null && counter <= sec.size()) {
			String line = content.get(lineNumber);
			if (line.startsWith(tab(counter)) || isCommented(line)) {
				if (line.startsWith(sec.get(counter))) {
					if (counter == sec.size()) {
						value = line.replace(sec.get(counter) + " ", "");
						break;
					}
					counter++;
				}
			} else {
				break;
			}
			lineNumber++;
		}
		return value;
	}

	public String getContents() {
		String value = "";
		for (int x = 1; x <= content.size(); x++) {
			value = value + content.get(x) + "\n";
		}
		return value;
	}

	public long getLong(String path) {
		return Long.parseLong(get(path).toString().trim());
	}

	public boolean pathExists(String path) {
		int counter = 1;
		int lineNumber = 1;
		while (content.get(lineNumber) != null && counter <= sec.size()) {
			String line = content.get(lineNumber);
			if (line.startsWith(tab(counter)) || isCommented(line)) {
				if (line.startsWith(sec.get(counter))) {
					if (counter == sec.size()) {
						return true;
					}
					counter++;
				}
			} else {
				break;
			}
			lineNumber++;
		}
		return false;
	}

	public List<?> getList(String path) {
		listings.clear();
		int counter = 1;
		int lineNumber = 1;
		section(counter, path);
		String line = null;
		while (content.get(lineNumber) != null) {
			line = content.get(lineNumber);
			if (line.startsWith(tab(counter))) {
				if (line.startsWith(sec.get(counter))) {
					if (counter == sec.size()) {
						lineNumber++;
						break;
					}
					counter++;
				}
			}
			lineNumber++;
		}
		line = content.get(lineNumber);
		while (line != null && (line.startsWith(tab(counter) + "- ") || isCommented(line))) {
			if (line.startsWith(tab(counter) + "- "))
				listings.add(line.trim().replaceFirst("- ", ""));
			lineNumber++;
			line = content.get(lineNumber);
		}
		return listings;
	}

	public List<String> getStringList(String path) {
		List<String> list = new ArrayList<String>();
		int counter = 0;
		getList(path);
		while (counter < listings.size()) {
			list.add(listings.get(counter).toString());
			counter++;
		}
		return list;
	}

	public String getString(String path) {
		if (get(path) != null) {
			return get(path).toString();
		}
		return null;
	}

	public boolean getBoolean(String path) {
		if (path == null) {
			System.err.println("Path is null, Returning false");
			return false;
		}
		return Boolean.parseBoolean(get(path).toString().trim());
	}

	public int getInt(String path) {
		return Integer.parseInt(get(path).toString().trim());
	}

	public DataFiles copyDefaults(InputStream is, boolean overWrite) {
		if (overWrite || !file.exists() || isEmpty()) {
			if (is == null) {
				System.out.println("[Warning] " + file.getName() + "'s .jar file have been modified!");
				System.out.println("[Warning] Could not generate " + file.getName() + "!");
				System.out.println("[Warning] Please stop and restart the server completely!");
				return this;
			}
			try {
				Files.copy(is, file.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return this;
	}

	public boolean isEmpty() {
		Scanner input;
		try {
			input = new Scanner(file);
			if (input.hasNextLine()) {
				input.close();
				return false;
			}
			input.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return true;
	}

	public void save() {
		try {
			FileWriter fw = new FileWriter(file, false);
			for (int x = 1; x <= content.size(); x++) {
				fw.write(content.get(x) + "\n");
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void reload() {
		try {
			File direc = new File(file.getPath().replace(file.getName(), ""));
			if (!direc.exists())
				direc.mkdirs();
			if (!file.exists())
				file.createNewFile();
			content.clear();
			int lineNumber = 0;
			Scanner input = new Scanner(file);
			while (input.hasNextLine()) {
				String line = input.nextLine();
				lineNumber++;
				content.put(lineNumber, line);
			}
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void delete() {
		file.delete();
	}

	private boolean isCommented(String line) {
		if (line.trim().startsWith("#") || line.trim().isEmpty())
			return true;
		return false;
	}

	private boolean isListing(String line) {
		if (line.trim().startsWith("- "))
			return true;
		return false;
	}

	private void section(int counter, String path) {
		sec.clear();
		if (path.isEmpty())
			return;
		String[] pathx = path.split("\\.");
		for (String x : pathx) {
			x = x + ":";
			sec.put(counter, tab(counter) + x);
			counter++;
		}
	}

	private String tab(int tabNumber) {
		String tab = "";
		for (int x = tabNumber; x > 1; x--) {
			tab = tab + "  ";
		}
		return tab;
	}

	public File getFile() {
		return file;
	}

	public void comment(String path, String comment) {
		int counter = 1;
		int lineNumber = 1;
		section(counter, path);
		while (content.get(lineNumber) != null && counter <= sec.size()) {
			String line = content.get(lineNumber);
			if (line.startsWith(tab(counter)) || isCommented(line)) {
				if (line.startsWith(sec.get(counter))) {
					if (counter == sec.size()) {
						for (int x = content.size(); x >= lineNumber; x--) {
							content.put(x + 1, content.get(x));
							content.remove(content.get(x));
						}
						content.put(lineNumber, tab(counter) + "# " + comment);
						break;
					}
					counter++;
				}
			} else {
				throw new NullPointerException();
			}
			lineNumber++;
		}
	}

	public void comment(String path, String[] comment) {
		if (pathExists(path))
			for (String c : comment)
				comment(path, c);
		else
			throw new NullPointerException();
	}

	public void removePath(String path) {
		int counter = 1;
		int lineNumber = 1;
		section(counter, path);
		while (content.get(lineNumber) != null && counter <= sec.size()) {
			String line = content.get(lineNumber);
			if (line.startsWith(tab(counter)) || isCommented(line)) {
				if (line.startsWith(sec.get(counter))) {
					if (counter == sec.size()) {
						int last = lineNumber;
						int diff = lineNumber;
						lineNumber++;
						while (content.get(lineNumber) != null && (isListing(content.get(lineNumber))
								|| content.get(lineNumber).startsWith(tab(counter + 1))
								|| isCommented(content.get(lineNumber)))) {
							if (!isCommented(content.get(lineNumber))) {
								diff = lineNumber;
							}
							lineNumber++;
						}
						lineNumber = diff + 1;
						for (int i = last; i < lineNumber; i++) {
							content.remove(i);
						}
						for (int i = last; content.get(lineNumber) != null; i++) {
							content.put(i, content.get(lineNumber));
							content.remove(lineNumber);
							lineNumber++;
						}
						return;
					}
					counter++;
				}
			} else {
				throw new NullPointerException();
			}
			lineNumber++;
		}
		throw new NullPointerException();
	}
	///////////////////////////////// Unused Methods
	///////////////////////////////// //////////////////////////////////
	/*
	 * public static Set<DataFiles> getFolderContents(String folderLoc) {
	 * Set<DataFiles> files = new HashSet<>(); File file = new File(folderLoc);
	 * if (!file.exists()) file.mkdirs(); if (file.isDirectory()) { for (File f
	 * : file.listFiles()) { files.add(new DataFiles(f)); } return files; }
	 * return null; }
	 * 
	 * public float getFloat(String path) { return
	 * Float.parseFloat(get(path).toString().trim()); }
	 * 
	 * public List<Double> getDoubleList(String path) { List<Double> list = new
	 * ArrayList<Double>(); getList(path); int counter = 0; while (counter <
	 * listings.size()) {
	 * list.add(Double.parseDouble(listings.get(counter).toString().trim()));
	 * counter++; } return list; }
	 * 
	 * public List<Boolean> getBooleanList(String path) { List<Boolean> list =
	 * new ArrayList<Boolean>(); getList(path); int counter = 0; while (counter
	 * < listings.size()) {
	 * list.add(Boolean.parseBoolean(listings.get(counter).toString().trim()));
	 * counter++; } return list; }
	 * 
	 * public double getDouble(String path) { return
	 * Double.parseDouble(get(path).toString().trim()); }
	 * 
	 * @Deprecated public DataFiles copyDefaults(String pathToResource, boolean
	 * overWrite) { InputStream is =
	 * (DataFiles.class.getClassLoader().getResourceAsStream(pathToResource));
	 * return copyDefaults(is, overWrite); }
	 * 
	 * public List<Integer> getIntList(String path) { List<Integer> list = new
	 * ArrayList<Integer>(); getList(path); int counter = 0; while (counter <
	 * listings.size()) {
	 * list.add(Integer.parseInt(listings.get(counter).toString().trim()));
	 * counter++; } return list; }
	 * 
	 * public List<Long> getLongList(String path) { List<Long> list = new
	 * ArrayList<Long>(); getList(path); int counter = 0; while (counter <
	 * listings.size()) {
	 * list.add(Long.parseLong(listings.get(counter).toString().trim()));
	 * counter++; } return list; }
	 * 
	 * 
	 * public int lineNumber(String path) { int counter = 1; int lineNumber = 1;
	 * section(counter, path); while (content.get(lineNumber) != null && counter
	 * <= sec.size()) { String line = content.get(lineNumber); if
	 * (line.startsWith(tab(counter)) || isCommented(line)) { if
	 * (line.startsWith(sec.get(counter))) { if (counter == sec.size()) { return
	 * lineNumber; } counter++; } } else { throw new NullPointerException(); }
	 * lineNumber++; } throw new NullPointerException(); }
	 * 
	 * public boolean removeComments(String path) { int counter = 1; int
	 * lineNumber = 1; section(counter, path); while (content.get(lineNumber) !=
	 * null && counter <= sec.size()) { String line = content.get(lineNumber);
	 * if (line.startsWith(tab(counter)) || isCommented(line)) { if
	 * (line.startsWith(sec.get(counter))) { if (counter == sec.size()) { int
	 * last = lineNumber - 1; while (isCommented(content.get(last))) { last--; }
	 * int diff = last - lineNumber + 1; if (diff > 0) { while
	 * (content.get(lineNumber) != null) { content.put(lineNumber + (diff),
	 * content.get(lineNumber)); content.remove(lineNumber); lineNumber++; }
	 * return true; } return false; } counter++; } } else { throw new
	 * NullPointerException(); } lineNumber++; } throw new
	 * NullPointerException(); }
	 * 
	 * public Set<String> getPaths(String path) { Set<String> paths = new
	 * HashSet<>(); int lineNumber = 1; int counter = 1; section(counter, path);
	 * while (content.get(lineNumber) != null && counter <= sec.size()) { String
	 * line = content.get(lineNumber); if (line.startsWith(tab(counter)) ||
	 * isCommented(line)) { if (line.startsWith(sec.get(counter))) { if (counter
	 * == sec.size()) { counter++; lineNumber++; break; } counter++; } } else {
	 * throw new NullPointerException(); } lineNumber++; } while
	 * (content.get(lineNumber) != null &&
	 * (content.get(lineNumber).startsWith(tab(counter)) ||
	 * isCommented(content.get(lineNumber)))) { if
	 * (!isCommented(content.get(lineNumber)) &&
	 * !content.get(lineNumber).startsWith(tab(counter + 1)) &&
	 * !isListing(content.get(lineNumber))) {
	 * paths.add(content.get(lineNumber).trim().substring(0,
	 * content.get(lineNumber).trim().indexOf(":"))); } lineNumber++; } return
	 * paths; }
	 * 
	 * public void rename(String path, String pathName) { int counter = 1; int
	 * lineNumber = 1; section(counter, path); while (content.get(lineNumber) !=
	 * null && counter <= sec.size()) { String line = content.get(lineNumber);
	 * if (line.startsWith(tab(counter)) || isCommented(line)) { if
	 * (line.startsWith(sec.get(counter))) { if (counter == sec.size()) {
	 * content.put(lineNumber, tab(counter) + pathName); break; } counter++; } }
	 * else { throw new NullPointerException(); } lineNumber++; } }
	 */
}
