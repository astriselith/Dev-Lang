package com.lang.json;

import com.lang.util.Position;
import java.util.*;

public class JsonObject extends Json {

	private final Map<String, Json> fields;

	public JsonObject(Map<String, Json> fields, Position position) {
		super(TYPE_OBJECT, position);
		this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
	}

	public JsonObject() {
		this(new LinkedHashMap<>(), null);
	}

	public Map<String, Json> getFields() {
		return fields;
	}

	public Json get(String key) {
		return fields.get(key);
	}

	public boolean has(String key) {
		return fields.containsKey(key);
	}

	public Set<String> keys() {
		return fields.keySet();
	}

	public int size() {
		return fields.size();
	}

	public boolean isEmpty() {
		return fields.isEmpty();
	}

	public Json opt(String key) {
		return fields.get(key);
	}

	public Json opt(String key, Json defaultValue) {
		Json result = fields.get(key);
		return result != null ? result : defaultValue;
	}

	public String optString(String key) {
		return optString(key, null);
	}

	public String optString(String key, String defaultValue) {
		Json result = fields.get(key);
		if (result != null && result.isString()) {
			return result.asString().getValue();
		}
		return defaultValue;
	}

	public int optInt(String key) {
		return optInt(key, 0);
	}

	public int optInt(String key, int defaultValue) {
		Json result = fields.get(key);
		if (result != null && result.isNumber()) {
			return result.asNumber().intValue();
		}
		return defaultValue;
	}

	public long optLong(String key) {
		return optLong(key, 0L);
	}

	public long optLong(String key, long defaultValue) {
		Json result = fields.get(key);
		if (result != null && result.isNumber()) {
			return result.asNumber().longValue();
		}
		return defaultValue;
	}

	public double optDouble(String key) {
		return optDouble(key, 0.0);
	}

	public double optDouble(String key, double defaultValue) {
		Json result = fields.get(key);
		if (result != null && result.isNumber()) {
			return result.asNumber().doubleValue();
		}
		return defaultValue;
	}

	public float optFloat(String key) {
		return optFloat(key, 0.0f);
	}

	public float optFloat(String key, float defaultValue) {
		Json result = fields.get(key);
		if (result != null && result.isNumber()) {
			return result.asNumber().floatValue();
		}
		return defaultValue;
	}

	public boolean optBoolean(String key) {
		return optBoolean(key, false);
	}

	public boolean optBoolean(String key, boolean defaultValue) {
		Json result = fields.get(key);
		if (result != null && result.isBoolean()) {
			return result.asBoolean().getValue();
		}
		return defaultValue;
	}

	public JsonObject optObject(String key) {
		return optObject(key, null);
	}

	public JsonObject optObject(String key, JsonObject defaultValue) {
		Json result = fields.get(key);
		if (result != null && result.isObject()) {
			return result.asObject();
		}
		return defaultValue;
	}

	public JsonArray optArray(String key) {
		return optArray(key, null);
	}

	public JsonArray optArray(String key, JsonArray defaultValue) {
		Json result = fields.get(key);
		if (result != null && result.isArray()) {
			return result.asArray();
		}
		return defaultValue;
	}

	@Override
	public String toString(int indent) {
		if (fields.isEmpty()) {
			return "{}";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		boolean first = true;
		for (Map.Entry<String, Json> entry : fields.entrySet()) {
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
		if (this == obj)
			return true;
		if (!(obj instanceof JsonObject))
			return false;
		return fields.equals(((JsonObject) obj).fields);
	}

	@Override
	public int hashCode() {
		return fields.hashCode();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final Map<String, Json> fields = new LinkedHashMap<>();
		private Position position;

		public Builder position(Position position) {
			this.position = position;
			return this;
		}

		public Builder put(String key, Json value) {
			fields.put(key, value);
			return this;
		}

		public Builder putAll(Map<String, Json> fields) {
			this.fields.putAll(fields);
			return this;
		}

		public JsonObject build() {
			return new JsonObject(fields, position);
		}
	}
}