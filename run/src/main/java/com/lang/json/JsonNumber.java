package com.lang.json;

import com.lang.util.Position;

public class JsonNumber extends Json {

	private final Number value;

	public JsonNumber(Number value, Position position) {
		super(TYPE_NUMBER, position);
		this.value = value;
	}

	public JsonNumber(Number value) {
		this(value, null);
	}

	public Number getValue() {
		return value;
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

	@Override
	public String toString(int indent) {
		return String.valueOf(value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof JsonNumber))
			return false;
		return Double.compare(doubleValue(), ((JsonNumber) obj).doubleValue()) == 0;
	}

	@Override
	public int hashCode() {
		return Double.hashCode(doubleValue());
	}
}