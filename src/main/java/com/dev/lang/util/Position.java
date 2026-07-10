package com.dev.lang.util;

public class Position implements Positioned {
	private final int line;
	private final int lineStart;
	private final int start;
	private final int end;

	public static final Position ZERO = new Position(0, 0, 0, 0);

	public Position(int line, int lineStart, int start, int end) {
		this.line = line;
		this.lineStart = lineStart;
		this.start = start;
		this.end = end;
	}

	public int getLine() {
		return line;
	}

	public int getLineStart() {
		return lineStart;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public int length() {
		return end - start;
	}

	@Override
	public Position getPosition() {
		return this;
	}

	public static Position between(Position start, Position end) {
		return new Position(start.line, start.lineStart, start.start, end.end);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Position that = (Position) obj;
		return line == that.line &&
			   lineStart == that.lineStart &&
			   start == that.start &&
			   end == that.end;
	}

	@Override
	public int hashCode() {
		int result = line;
		result = 31 * result + lineStart;
		result = 31 * result + start;
		result = 31 * result + end;
		return result;
	}

	@Override
	public String toString() {
		return String.format("line %d, lineStart %d, pos %d-%d", line, lineStart, start, end);
	}
}