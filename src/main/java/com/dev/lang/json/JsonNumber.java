package com.dev.lang.json;

import com.dev.lang.util.Position;

public class JsonNumber extends Json<Number> {
	public JsonNumber(Number value, Position position) {
		super(value, TYPE_NUMBER, position);
	}

	public JsonNumber(Number value) {
		this(value, null);
	}

	public int intValue() {
		return value.intValue();
	}
	public long longValue() {
		return value.longValue();
	}
	public double doubleValue() {
		return value.doubleValue();
	}
	public float floatValue() {
		return value.floatValue();
	}

	public boolean isIntegral() {
		return value instanceof Integer || value instanceof Long;
	}

	@Override
	public String toString(int indent) {
		if (isIntegral()) {
			return String.valueOf(value.longValue());
		}
		String s = value.toString();
		return s.contains(".") ? s : s + ".0";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof JsonNumber)) return false;
		return Double.compare(doubleValue(), ((JsonNumber) obj).doubleValue()) == 0;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(doubleValue());
	}
}