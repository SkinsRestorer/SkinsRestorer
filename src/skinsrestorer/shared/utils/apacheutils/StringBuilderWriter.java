package skinsrestorer.shared.utils.apacheutils;

import java.io.Serializable;
import java.io.Writer;

public class StringBuilderWriter extends Writer implements Serializable {

	private static final long serialVersionUID = 658835771565808301L;

	private final StringBuilder builder = new StringBuilder();

	@Override
	public Writer append(final char value) {
		this.builder.append(value);
		return this;
	}

	@Override
	public Writer append(final CharSequence value) {
		this.builder.append(value);
		return this;
	}

	@Override
	public Writer append(final CharSequence value, final int start, final int end) {
		this.builder.append(value, start, end);
		return this;
	}

	@Override
	public void close() {
	}

	@Override
	public void flush() {
	}

	@Override
	public void write(final String value) {
		if (value != null) {
			this.builder.append(value);
		}
	}

	@Override
	public void write(final char[] value, final int offset, final int length) {
		if (value != null) {
			this.builder.append(value, offset, length);
		}
	}

	public StringBuilder getBuilder() {
		return this.builder;
	}

	@Override
	public String toString() {
		return this.builder.toString();
	}

}
