package skinsrestorer.libs.org.json.simple.parser;

import java.io.IOException;

public interface ContentHandler {

	void startJSON() throws ParseException, IOException;

	void endJSON() throws ParseException, IOException;

	boolean startObject() throws ParseException, IOException;

	boolean endObject() throws ParseException, IOException;

	boolean startObjectEntry(String key) throws ParseException, IOException;

	boolean endObjectEntry() throws ParseException, IOException;

	boolean startArray() throws ParseException, IOException;

	boolean endArray() throws ParseException, IOException;

	boolean primitive(Object value) throws ParseException, IOException;

}
