package com.dev.lang.json;

import com.dev.lang.util.Position;

public abstract class Json<T> {
	protected final T value;
	protected final int type;
	protected final Position position;

	public static final int TYPE_NULL = 0;
	public static final int TYPE_BOOLEAN = 1;
	public static final int TYPE_NUMBER = 2;
	public static final int TYPE_STRING = 3;
	public static final int TYPE_ARRAY = 4;
	public static final int TYPE_OBJECT = 5;

	private static final String INDENT_STRING = "  ";

	protected Json(T value, int type, Position position) {
		this.value = value;
		this.type = type;
		this.position = position;
	}

	public T getValue() {
		return value;
	}
	public int getType() {
		return type;
	}
	public Position getPosition() {
		return position;
	}

	public String getTypeName() {
		switch (type) {
		case TYPE_NULL:
			return "null";
		case TYPE_BOOLEAN:
			return "boolean";
		case TYPE_NUMBER:
			return "number";
		case TYPE_STRING:
			return "string";
		case TYPE_ARRAY:
			return "array";
		case TYPE_OBJECT:
			return "object";
		default:
			return "unknown";
		}
	}

	public boolean isNull() {
		return type == TYPE_NULL;
	}
	public boolean isBoolean() {
		return type == TYPE_BOOLEAN;
	}
	public boolean isNumber() {
		return type == TYPE_NUMBER;
	}
	public boolean isString() {
		return type == TYPE_STRING;
	}
	public boolean isArray() {
		return type == TYPE_ARRAY;
	}
	public boolean isObject() {
		return type == TYPE_OBJECT;
	}

	public JsonNull asNull() {
		return (JsonNull) this;
	}
	public JsonBoolean asBoolean() {
		return (JsonBoolean) this;
	}
	public JsonNumber asNumber() {
		return (JsonNumber) this;
	}
	public JsonString asString() {
		return (JsonString) this;
	}
	public JsonArray asArray() {
		return (JsonArray) this;
	}
	public JsonObject asObject() {
		return (JsonObject) this;
	}

	public static String indent(int level) {
		return INDENT_STRING.repeat(Math.max(0, level));
	}

	@Override
	public String toString() {
		return toString(0);
	}

	public abstract String toString(int indent);
}