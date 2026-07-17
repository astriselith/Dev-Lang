package com.lang.json;

import com.lang.util.Position;
import java.util.*;

public class JsonArray extends Json<List<Json<?>>> {
	public JsonArray(List<Json<?>> elements, Position position) {
		super(Collections.unmodifiableList(new ArrayList<>(elements)), TYPE_ARRAY, position);
	}

	public JsonArray() {
		this(new ArrayList<>(), null);
	}

	public List<Json<?>> getElements() {
		return value;
	}
	public int size() {
		return value.size();
	}
	public boolean isEmpty() {
		return value.isEmpty();
	}
	public Json<?> get(int index) {
		return value.get(index);
	}

	@Override
	public String toString(int indent) {
		if (value.isEmpty()) {
			return "[]";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("[\n");
		for (int i = 0; i < value.size(); i++) {
			sb.append(indent(indent + 1));
			sb.append(value.get(i).toString(indent + 1));
			if (i < value.size() - 1) {
				sb.append(",");
			}
			sb.append("\n");
		}
		sb.append(indent(indent));
		sb.append("]");
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof JsonArray)) return false;
		return value.equals(((JsonArray) obj).value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final List<Json<?>> elements = new ArrayList<>();
		private Position position;

		public Builder position(Position position) {
			this.position = position;
			return this;
		}

		public Builder add(Json<?> element) {
			elements.add(element);
			return this;
		}

		public Builder addAll(List<Json<?>> elements) {
			this.elements.addAll(elements);
			return this;
		}

		public JsonArray build() {
			return new JsonArray(elements, position);
		}
	}
}