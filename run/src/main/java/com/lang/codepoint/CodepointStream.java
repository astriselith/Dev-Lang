package com.lang.codepoint;

import com.lang.buffer.ObjectBuffer;
import java.io.IOException;

public abstract class CodepointStream extends ObjectBuffer<Integer> {

	protected CodepointStream() {
		this(10);
	}

	protected CodepointStream(int sidedWindow) {
		super(sidedWindow);
	}

	@Override
	protected abstract Integer fetchNext();

	@Override
	protected boolean isEOF(Integer codepoint) {
		return codepoint == null || codepoint == Codepoint.EOF;
	}

	public int peek() {
		Integer value = offset(0);
		return value != null ? value : Codepoint.EOF;
	}

	public int peekNext() {
		Integer value = offset(1);
		return value != null ? value : Codepoint.EOF;
	}

	public int peekNextNext() {
		Integer value = offset(2);
		return value != null ? value : Codepoint.EOF;
	}

	public int peekBack() {
		Integer value = offset(-1);
		return value != null ? value : Codepoint.EOF;
	}

	public int peekBackBack() {
		Integer value = offset(-2);
		return value != null ? value : Codepoint.EOF;
	}

	public int advance() {
		Integer current = offset(0);
		if (current != null && current != Codepoint.EOF) {
			next();
			return current;
		}
		return Codepoint.EOF;
	}

	public boolean check(int value) {
		Integer current = offset(0);
		return current != null && current == value;
	}

	public boolean check(int offset, int value) {
		Integer current = offset(offset);
		return current != null && current == value;
	}

	public boolean checkAny(int... values) {
		Integer current = offset(0);
		if (current == null)
			return false;
		for (int v : values) {
			if (current == v)
				return true;
		}
		return false;
	}

	public boolean match(int value) {
		if (check(value)) {
			next();
			return true;
		}
		return false;
	}

	public boolean matchAny(int... values) {
		for (int v : values) {
			if (check(v)) {
				next();
				return true;
			}
		}
		return false;
	}

	public int expect(int value) {
		int current = peek();

		if (current != value) {
			throw new IllegalStateException(
					"Expected '" +
							Codepoint.toString(value) +
							"' but found '" +
							Codepoint.toString(current) +
							"'");
		}

		advance();
		return current;
	}

	public int expectAny(int... values) {
		int current = peek();

		for (int value : values) {
			if (current == value) {
				advance();
				return current;
			}
		}

		StringBuilder expected = new StringBuilder();

		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				expected.append(", ");
			}
			expected.append('\'')
					.append(Codepoint.toString(values[i]))
					.append('\'');
		}

		throw new IllegalStateException(
				"Expected one of [" +
						expected +
						"] but found '" +
						Codepoint.toString(current) +
						"'");
	}

	public boolean isAtEnd() {
		return !hasNext();
	}

	public void reset() {
		release();
	}

	public abstract void close() throws IOException;
}