package com.dev.lang.ast;

import com.dev.lang.util.Position;

public class Modifier extends Node {
	// Bit flags
	public static final int SHARED   = 1 << 0;
	public static final int READONLY = 1 << 1;
	public static final int PUBLIC   = 1 << 2;
	public static final int PRIVATE  = 1 << 3;

	public static final int ALL = SHARED | READONLY | PUBLIC | PRIVATE;

	private final int flags;

	public Modifier(int flags, Position position) {
		super(position);
		this.flags = flags & ALL;
	}

	public Modifier(Position position) {
		this(0, position);
	}

	public int getFlags() {
		return flags;
	}

	public boolean has() {
		return has(ALL);
	}
	public boolean has(int flag) {
		return (flags & flag) != 0;
	}

	public boolean isShared() {
		return has(SHARED);
	}

	public boolean isReadonly() {
		return has(READONLY);
	}

	public boolean isPublic() {
		return has(PUBLIC);
	}

	public boolean isPrivate() {
		return has(PRIVATE);
	}

	public boolean isValid() {
		return !(has(PUBLIC) && has(PRIVATE));
	}

	public String getName() {
		if (!isValid()) return "";

		StringBuilder sb = new StringBuilder();

		if (has(SHARED)) sb.append("shared ");
		if (has(READONLY)) sb.append("readonly ");
		if (has(PUBLIC)) sb.append("public ");
		if (has(PRIVATE)) sb.append("private ");

		return sb.toString().trim();
	}

	public static int addFlag(int currentFlags, int flag) {
		if ((flag == PUBLIC || flag == PRIVATE) && (currentFlags & (PUBLIC | PRIVATE)) != 0) {
			currentFlags &= ~(PUBLIC | PRIVATE);
		}
		return currentFlags | flag;
	}

	public static int removeFlag(int currentFlags, int flag) {
		return currentFlags & ~flag;
	}

	public static boolean hasFlag(int flags, int flag) {
		return (flags & flag) != 0;
	}

	public Modifier combine(Modifier other) {
		int newFlags = this.flags | other.flags;

		if (hasFlag(newFlags, PUBLIC)) {
			newFlags &= ~PRIVATE;
		}
		return new Modifier(newFlags,
							Position.between(this.getPosition(), other.getPosition()));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Modifier)) return false;
		Modifier other = (Modifier) obj;
		return flags == other.flags;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(flags);
	}

	@Override
	public String toString() {
		String name = getName();
		return name.isEmpty() ? "" : name;
	}
}