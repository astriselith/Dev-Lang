package com.lang.json;

import com.lang.util.Position;

public class JsonNull extends Json {

	public JsonNull(Position position) {
		super(TYPE_NULL, position);
	}

	private JsonNull() {
		this(null);
	}

	@Override
	public String toString(int indent) {
		return "null";
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this || obj instanceof JsonNull;
	}

	@Override
	public int hashCode() {
		return 0;
	}
}