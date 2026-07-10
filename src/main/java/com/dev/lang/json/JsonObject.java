package com.dev.lang.json;

import com.dev.lang.util.Position;
import java.util.*;

public class JsonObject extends Json<Map<String, Json<?>>> {
	public JsonObject(Map<String, Json<?>> fields, Position position) {
		super(Collections.unmodifiableMap(new LinkedHashMap<>(fields)), TYPE_OBJECT, position);
	}

	public JsonObject() {
		this(new LinkedHashMap<>(), null);
	}

	public Map<String, Json<?>> getFields() {
		return value;
	}
	public Json<?> get(String key) {
		return value.get(key);
	}
	public boolean has(String key) {
		return value.containsKey(key);
	}
	public Set<String> keys() {
		return value.keySet();
	}
	public int size() {
		return value.size();
	}
	public boolean isEmpty() {
		return value.isEmpty();
	}

	public Json<?> opt(String key) {
		return value.get(key);
	}

	public Json<?> opt(String key, Json<?> defaultValue) {
		Json<?> result = value.get(key);
		return result != null ? result : defaultValue;
	}

	public String optString(String key) {
		return optString(key, null);
	}

	public String optString(String key, String defaultValue) {
		Json<?> result = value.get(key);
		if (result != null && result.isString()) {
			return result.asString().getValue();
		}
		return defaultValue;
	}

	public int optInt(String key) {
		return optInt(key, 0);
	}

	public int optInt(String key, int defaultValue) {
		Json<?> result = value.get(key);
		if (result != null && result.isNumber()) {
			return result.asNumber().intValue();
		}
		return defaultValue;
	}

	public long optLong(String key) {
		return optLong(key, 0L);
	}

	public long optLong(String key, long defaultValue) {
		Json<?> result = value.get(key);
		if (result != null && result.isNumber()) {
			return result.asNumber().longValue();
		}
		return defaultValue;
	}

	public double optDouble(String key) {
		return optDouble(key, 0.0);
	}

	public double optDouble(String key, double defaultValue) {
		Json<?> result = value.get(key);
		if (result != null && result.isNumber()) {
			return result.asNumber().doubleValue();
		}
		return defaultValue;
	}

	public float optFloat(String key) {
		return optFloat(key, 0.0f);
	}

	public float optFloat(String key, float defaultValue) {
		Json<?> result = value.get(key);
		if (result != null && result.isNumber()) {
			return result.asNumber().floatValue();
		}
		return defaultValue;
	}

	public boolean optBoolean(String key) {
		return optBoolean(key, false);
	}

	public boolean optBoolean(String key, boolean defaultValue) {
		Json<?> result = value.get(key);
		if (result != null && result.isBoolean()) {
			return result.asBoolean().getValue();
		}
		return defaultValue;
	}

	public JsonObject optObject(String key) {
		return optObject(key, null);
	}

	public JsonObject optObject(String key, JsonObject defaultValue) {
		Json<?> result = value.get(key);
		if (result != null && result.isObject()) {
			return result.asObject();
		}
		return defaultValue;
	}

	public JsonArray optArray(String key) {
		return optArray(key, null);
	}

	public JsonArray optArray(String key, JsonArray defaultValue) {
		Json<?> result = value.get(key);
		if (result != null && result.isArray()) {
			return result.asArray();
		}
		return defaultValue;
	}

	@Override
	public String toString(int indent) {
		if (value.isEmpty()) {
			return "{}";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		boolean first = true;
		for (Map.Entry<String, Json<?>> entry : value.entrySet()) {
			if (!first) {
				sb.append(",\n");
			}
			first = false;
			sb.append(indent(indent + 1));
			sb.append("\"").append(entry.getKey()).append("\": ");
			sb.append(entry.getValue().toString(indent + 1));
		}
		sb.append("\n");
		sb.append(indent(indent));
		sb.append("}");
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof JsonObject)) return false;
		return value.equals(((JsonObject) obj).value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final Map<String, Json<?>> fields = new LinkedHashMap<>();
		private Position position;

		public Builder position(Position position) {
			this.position = position;
			return this;
		}

		public Builder put(String key, Json<?> value) {
			fields.put(key, value);
			return this;
		}

		public Builder put(String key, String value) {
			fields.put(key, new JsonString(value));
			return this;
		}

		public Builder put(String key, int value) {
			fields.put(key, new JsonNumber(value));
			return this;
		}

		public Builder put(String key, long value) {
			fields.put(key, new JsonNumber(value));
			return this;
		}

		public Builder put(String key, double value) {
			fields.put(key, new JsonNumber(value));
			return this;
		}

		public Builder put(String key, float value) {
			fields.put(key, new JsonNumber(value));
			return this;
		}

		public Builder put(String key, boolean value) {
			fields.put(key, new JsonBoolean(value));
			return this;
		}

		public Builder putNull(String key) {
			fields.put(key, JsonNull.INSTANCE);
			return this;
		}

		public Builder putAll(Map<String, Json<?>> fields) {
			this.fields.putAll(fields);
			return this;
		}

		public JsonObject build() {
			return new JsonObject(fields, position);
		}
	}
}