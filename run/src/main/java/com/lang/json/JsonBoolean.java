package com.lang.json;

import java.util.Objects;

import com.lang.util.Position;

public class JsonBoolean extends Json {
	private final boolean value;

	public JsonBoolean(boolean value, Position position) {
		super(TYPE_BOOLEAN, position);
		this.value = value;
	}

	public JsonBoolean(boolean value) {
		this(value, null);
	}

	public boolean getValue() {
		return value;
	}

	@Override
	public String toString(int indent) {
		return String.valueOf(value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof JsonBoolean))
			return false;

		return value == ((JsonBoolean) obj).value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
}