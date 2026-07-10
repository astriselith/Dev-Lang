package com.dev.lang.json;

import com.dev.lang.util.Position;

public class JsonString extends Json<String> {
	public JsonString(String value, Position position) {
		super(value, TYPE_STRING, position);
	}

	public JsonString(String value) {
		this(value, null);
	}

	@Override
	public String toString(int indent) {
		return "\"" + escape(value) + "\"";
	}

	private static String escape(String s) {
		StringBuilder sb = new StringBuilder();
		for (char c : s.toCharArray()) {
			switch (c) {
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
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
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			default:
				if (c < 0x20) {
					sb.append(String.format("\\u%04x", (int) c));
				} else {
					sb.append(c);
				}
				break;
			}
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof JsonString)) return false;
		return value.equals(((JsonString) obj).value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}