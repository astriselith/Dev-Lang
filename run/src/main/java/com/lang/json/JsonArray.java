package com.lang.json;

import com.lang.util.Position;
import java.util.*;

public class JsonArray extends Json {
	private final List<Json> elements;

	public JsonArray(List<Json> elements, Position position) {
		super(TYPE_ARRAY, position);
		this.elements = Collections.unmodifiableList(new ArrayList<>(elements));

	}

	public JsonArray() {
		this(new ArrayList<>(), null);
	}

	public List<Json> getElements() {
		return elements;
	}

	public int size() {
		return elements.size();
	}

	public boolean isEmpty() {
		return elements.isEmpty();
	}

	public Json get(int index) {
		return elements.get(index);
	}

	@Override
	public String toString(int indent) {
		if (elements.isEmpty()) {
			return "[]";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("[\n");
		for (int i = 0; i < elements.size(); i++) {
			sb.append(indent(indent + 1));
			sb.append(elements.get(i).toString(indent + 1));
			if (i < elements.size() - 1) {
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
		if (this == obj)
			return true;
		if (!(obj instanceof JsonArray))
			return false;
		return elements.equals(((JsonArray) obj).elements);
	}

	@Override
	public int hashCode() {
		return elements.hashCode();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final List<Json> elements = new ArrayList<>();
		private Position position;

		public Builder position(Position position) {
			this.position = position;
			return this;
		}

		public Builder add(Json element) {
			elements.add(element);
			return this;
		}

		public Builder addAll(List<Json> elements) {
			this.elements.addAll(elements);
			return this;
		}

		public JsonArray build() {
			return new JsonArray(elements, position);
		}
	}
}