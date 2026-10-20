package com.lang.codepoint;

import java.io.IOException;

public abstract class CodepointStream {

    protected final int window;
    private final int[] buffer;

    private int head = 0;
    private int tail = -1;
    private boolean hitEof = false;
    private int eofIndex = -1;

    protected CodepointStream() {
        this(10);
    }

    protected CodepointStream(int window) {
        if (window <= 0) {
            throw new IllegalArgumentException("window must be > 0");
        }
        this.window = Math.max(window, 3);
        this.buffer = new int[(this.window * 2) + 1];
    }

    protected abstract int fetchNext();

    public abstract void close() throws IOException;

    private void maintainWindow() {
        int neededPhysicalIndex = head + window;

        if (neededPhysicalIndex >= buffer.length && !hitEof) {
            int startCopySource = Math.max(0, head - window);
            int elementsToKeep = (tail - startCopySource) + 1;

            if (elementsToKeep > 0) {
                System.arraycopy(buffer, startCopySource, buffer, 0, elementsToKeep);
            }

            int displacement = startCopySource;
            this.head -= displacement;
            this.tail -= displacement;
            if (eofIndex >= 0) {
                eofIndex -= displacement;
            }

            for (int i = Math.max(0, elementsToKeep); i < buffer.length; i++) {
                buffer[i] = 0;
            }
        }

        while (tail < (head + window) && !hitEof) {
            if (tail + 1 >= buffer.length) {
                break;
            }

            int next = fetchNext();

            if (next == Codepoint.EOF) {
                tail++;
                buffer[tail] = Codepoint.EOF;
                hitEof = true;
                eofIndex = tail;
                break;
            }

            tail++;
            buffer[tail] = next;
        }
    }

    protected int offset(int index) {
        if (index < -window || index > window) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " out of allowed window ["
                            + (-window) + ", " + window + "]");
        }

        maintainWindow();

        int target = head + index;

        if (target < 0) {
            throw new IndexOutOfBoundsException("Index " + target + " is negative (outside history).");
        }

        if (target > tail) {
            if (hitEof) {
                return Codepoint.EOF;
            }
            throw new IndexOutOfBoundsException(
                    "Index " + target + " not yet loaded or out of bounds. Tail: " + tail);
        }

        return buffer[target];
    }

    public int peek() {
        return offset(0);
    }

    protected int peekIndex(int index) {
        try {
            return offset(index);
        } catch (IndexOutOfBoundsException e) {
            return Codepoint.EOF;
        }
    }

    public int advance() {
        int current = offset(0);
        if (current != Codepoint.EOF) {
            head++;
        }
        return current;
    }

    public boolean isAtEnd() {
        maintainWindow();
        if (hitEof && head >= eofIndex) {
            return true;
        }
        return head > tail;
    }

    public boolean check(int value) {
        return peek() == value;
    }

    public boolean checkIndex(int index, int value) {
        return peekIndex(index) == value;
    }

    public boolean checkAny(int... values) {
        if (values == null || values.length == 0) return false;
        int v = peek();
        for (int x : values) {
            if (x == v) return true;
        }
        return false;
    }

    public boolean checkIndexAny(int index, int... values) {
        if (values == null || values.length == 0) return false;
        int v = peekIndex(index);
        for (int x : values) {
            if (x == v) return true;
        }
        return false;
    }

    public boolean checkSequence(int... values) {
        if (values == null || values.length == 0) return true;
        for (int i = 0; i < values.length; i++) {
            if (!checkIndex(i, values[i])) return false;
        }
        return true;
    }

    public boolean checkIndexSequence(int index, int... values) {
        if (values == null || values.length == 0) return true;
        for (int i = 0; i < values.length; i++) {
            if (!checkIndex(index + i, values[i])) return false;
        }
        return true;
    }

    public boolean match(int value) {
        if (check(value)) {
            advance();
            return true;
        }
        return false;
    }

    public boolean matchAny(int... values) {
        for (int v : values) {
            if (check(v)) {
                advance();
                return true;
            }
        }
        return false;
    }

    public boolean matchSequence(int... values) {
        if (values == null || values.length == 0) return true;
        if (!checkSequence(values)) return false;
        for (int i = 0; i < values.length; i++) {
            advance();
        }
        return true;
    }

    public int getHead() {
        return head;
    }

    public int getTail() {
        return tail;
    }

    public int getSize() {
        return buffer.length;
    }

    public void reset() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = 0;
        }
        head = 0;
        tail = -1;
        hitEof = false;
        eofIndex = -1;
    }
}