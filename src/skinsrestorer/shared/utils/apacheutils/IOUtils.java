package skinsrestorer.shared.utils.apacheutils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

public class IOUtils {

	public static String toString(InputStream is, Charset charset) throws IOException {
		InputStreamReader input = new InputStreamReader(is, charset);
		StringBuilderWriter sw = new StringBuilderWriter();
        copy(input, sw);
        return sw.toString();
	}

    private static int copy(final Reader input, final Writer output) throws IOException {
        final long count = copyLarge(input, output);
        if (count > 2147483647L) {
            return -1;
        }
        return (int)count;
    }
    
    private static long copyLarge(final Reader input, final Writer output) throws IOException {
        return copyLarge(input, output, new char[4096]);
    }
    
    private static long copyLarge(final Reader input, final Writer output, final char[] buffer) throws IOException {
        long count = 0L;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

	public static void closeQuietly(InputStream is) {
		if (is == null) {
			return;
		}
		try {
			is.close();
		} catch (IOException e) {
		}
	}

}
