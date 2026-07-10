package com.dev.lang.json;

import com.dev.lang.util.Position;

public class JsonNull extends Json<Void> {
	public static final JsonNull INSTANCE = new JsonNull();

	private JsonNull() {
		super(null, TYPE_NULL, null);
	}

	public JsonNull(Position position) {
		super(null, TYPE_NULL, position);
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