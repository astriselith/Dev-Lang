package com.lang.util;

public class Position {
    private final int firstLineCount;
    private final int firstLineStart;
    private final int start;

    private final int lastLineCount;
    private final int lastLineStart;
    private final int end;

    public static final Position ZERO = new Position(0, 0, 0, 0, 0, 0);
    public static final Position UNKNOWN = new Position(-1, -1, -1, -1, -1, -1);

    public Position(int firstLineCount, int firstLineStart, int start,
                    int lastLineCount, int lastLineStart, int end) {
        this.firstLineCount = firstLineCount;
        this.firstLineStart = firstLineStart;
        this.start = start;
        this.lastLineCount = lastLineCount;
        this.lastLineStart = lastLineStart;
        this.end = end;
    }

    public int getFirstLineCount() { return firstLineCount; }
    public int getFirstLineStart() { return firstLineStart; }
    public int getStart()          { return start; }

    public int getLastLineCount()  { return lastLineCount; }
    public int getLastLineStart()  { return lastLineStart; }
    public int getEnd()            { return end; }

    public int getColumnStart()    { return start - firstLineStart; }
    public int getColumnEnd()      { return end - lastLineStart; }

    public int length()            { return end - start; }

    public static Position between(Position start, Position end) {
        return new Position(
                start.firstLineCount, start.firstLineStart, start.start,
                end.lastLineCount, end.lastLineStart, end.end);
    }

    @Override
    public String toString() {
        return String.format("line %d-%d, col %d-%d",
                firstLineCount, lastLineCount, getColumnStart(), getColumnEnd());
    }
}