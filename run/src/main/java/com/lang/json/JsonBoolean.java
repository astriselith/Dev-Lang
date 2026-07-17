package com.lang.json;

import com.lang.util.Position;

public class JsonBoolean extends Json<Boolean> {
	public JsonBoolean(boolean value, Position position) {
		super(value, TYPE_BOOLEAN, position);
	}

	public JsonBoolean(boolean value) {
		this(value, null);
	}

	@Override
	public String toString(int indent) {
		return value.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof JsonBoolean)) return false;
		return value.equals(((JsonBoolean) obj).value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}
}