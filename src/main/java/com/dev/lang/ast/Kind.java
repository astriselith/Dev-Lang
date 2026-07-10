package com.dev.lang.ast;

public class Kind {
	// Bit flags
	public static final int CLASS = 1 << 0;
	public static final int TRAIT = 1 << 1;

	public static final int ALL = CLASS | TRAIT;

	private final int kind;

	public Kind(int kind) {
		this.kind = kind & ALL;
	}

	public Kind() {
		this(0);
	}

	public int getKind() {
		return kind;
	}

	public boolean has(int flag) {
		return (kind & flag) != 0;
	}

	public boolean isClass() {
		return has(CLASS);
	}

	public boolean isTrait() {
		return has(TRAIT);
	}

	public boolean isValid() {
		return has(CLASS) != has(TRAIT) || kind == 0;
	}

	public String getName() {
		if (!isValid()) return "";
		if (isClass()) return "class";
		if (isTrait()) return "trait";
		return "";
	}

	public static Kind combine(Kind first, Kind second) {
		int newKind = first.kind | second.kind;
		return new Kind(newKind);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Kind)) return false;
		Kind other = (Kind) obj;
		return kind == other.kind;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(kind);
	}

	@Override
	public String toString() {
		return getName();
	}
}