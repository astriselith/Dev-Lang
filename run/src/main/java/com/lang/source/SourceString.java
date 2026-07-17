package com.lang.source;

import com.lang.codepoint.Codepoint;
import com.lang.codepoint.CodepointStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SourceString extends CodepointStream {

	private final String source;
	private int position = 0;
	private final byte[] bytes;

	public SourceString(String source) {
		this.source = source;
		this.bytes = source.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	protected Integer fetchNext() {
		if (position >= bytes.length) {
			return Codepoint.EOF;
		}

		byte b = bytes[position++];
		int width = Codepoint.width(b);

		if (width == 0) {
			return 0xFFFD;
		}

		if (width == 1) {
			return b & 0xFF;
		}

		int codepoint = 0;

		switch (width) {
		case 2:
			codepoint = b & 0x1F;
			break;
		case 3:
			codepoint = b & 0x0F;
			break;
		case 4:
			codepoint = b & 0x07;
			break;
		}

		for (int i = 1; i < width; i++) {
			if (position >= bytes.length) {
				return 0xFFFD;
			}
			byte next = bytes[position++];
			if (!Codepoint.isContinuation(next)) {
				return 0xFFFD;
			}
			codepoint = (codepoint << 6) | (next & 0x3F);
		}

		if (!Codepoint.isValid(codepoint)) {
			return 0xFFFD;
		}

		return codepoint;
	}

	@Override
	public void close() throws IOException {
	}

	public String getSource() {
		return source;
	}

	public int getPosition() {
		return position;
	}
}