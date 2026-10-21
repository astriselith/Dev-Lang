package com.lang.token;

import java.io.IOException;
import java.util.function.Predicate;

public abstract class TokenStream {

    protected final int window;
    private final Token[] buffer;

    private int head = 0;
    private int tail = -1;
    private boolean hitEof = false;
    private int eofIndex = -1;

    private Predicate<Token> filter = t -> true;

    protected TokenStream() {
        this(10);
    }

    protected TokenStream(int window) {
        if (window <= 0) {
            throw new IllegalArgumentException("window must be > 0");
        }
        this.window = Math.max(window, 3);
        this.buffer = new Token[(this.window * 2) + 1];
    }

    public void setFilter(Predicate<Token> filter) {
        this.filter = filter != null ? filter : t -> true;
    }

    protected abstract Token fetchNext();

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
                buffer[i] = null;
            }
        }

        while (tail < (head + window) && !hitEof) {
            if (tail + 1 >= buffer.length) {
                break;
            }

            Token next = fetchNext();

            if (next != null && next.type != Token.EOF && !filter.test(next)) {
                continue;
            }

            tail++;
            buffer[tail] = next;

            if (next.type == Token.EOF) {
                hitEof = true;
                eofIndex = tail;
                break;
            }
        }
    }

    protected Token offset(int index) {
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
                return Token.EMPTY;
            }
            throw new IndexOutOfBoundsException(
                    "Index " + target + " not yet loaded or out of bounds. Tail: " + tail);
        }

        return buffer[target];
    }

    public Token peek() {
        return peekIndex(0);
    }

    public Token peekIndex(int index) {
        try {
            Token token = offset(index);
            return token != null ? token : Token.EMPTY;
        } catch (IndexOutOfBoundsException e) {
            return Token.EMPTY;
        }
    }

    public Token advance() {
        Token current = offset(0);
        if (current != null && !current.is(Token.EOF)) {
            head++;
        }
        return current;
    }

    public Token previous() {
        try {
            Token token = offset(-1);
            return token != null ? token : Token.EMPTY;
        } catch (IndexOutOfBoundsException e) {
            return Token.EMPTY;
        }
    }

    public boolean isAtEnd() {
        maintainWindow();
        if (hitEof && head >= eofIndex) {
            return true;
        }
        return head > tail;
    }

    public boolean check(int type) {
        return peek().typeEquals(type);
    }

    public boolean checkIndex(int index, int type) {
        return peekIndex(index).typeEquals(type);
    }

    public boolean checkAny(int... types) {
        if (types == null || types.length == 0)
            return false;
        int type = peek().type;
        for (int t : types) {
            if (t == type)
                return true;
        }
        return false;
    }

    public boolean checkIndexAny(int index, int... types) {
        if (types == null || types.length == 0)
            return false;
        int type = peekIndex(index).type;
        for (int t : types) {
            if (t == type)
                return true;
        }
        return false;
    }

    public boolean checkSequence(int... types) {
        if (types == null || types.length == 0)
            return true;
        for (int i = 0; i < types.length; i++) {
            if (!checkIndex(i, types[i]))
                return false;
        }
        return true;
    }

    public boolean checkIndexSequence(int index, int... types) {
        if (types == null || types.length == 0)
            return true;
        for (int i = 0; i < types.length; i++) {
            if (!checkIndex(index + i, types[i]))
                return false;
        }
        return true;
    }

    public boolean check(String lexeme) {
        return peek().lexemeEquals(lexeme);
    }

    public boolean checkIndex(int index, String lexeme) {
        return peekIndex(index).lexemeEquals(lexeme);
    }

    public boolean checkAny(String... lexemes) {
        if (lexemes == null || lexemes.length == 0)
            return false;
        String lexeme = peek().lexeme;
        for (String l : lexemes) {
            if (l != null && l.equals(lexeme))
                return true;
        }
        return false;
    }

    public boolean checkIndexAny(int index, String... lexemes) {
        if (lexemes == null || lexemes.length == 0)
            return false;
        String lexeme = peekIndex(index).lexeme;
        for (String l : lexemes) {
            if (l != null && l.equals(lexeme))
                return true;
        }
        return false;
    }

    public boolean checkSequence(String... lexemes) {
        if (lexemes == null || lexemes.length == 0)
            return true;
        for (int i = 0; i < lexemes.length; i++) {
            if (!checkIndex(i, lexemes[i]))
                return false;
        }
        return true;
    }

    public boolean checkIndexSequence(int index, String... lexemes) {
        if (lexemes == null || lexemes.length == 0)
            return true;
        for (int i = 0; i < lexemes.length; i++) {
            if (!checkIndex(index + i, lexemes[i]))
                return false;
        }
        return true;
    }

    public boolean match(int type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    public boolean matchAny(int... types) {
        if (types == null || types.length == 0)
            return false;
        for (int t : types) {
            if (check(t)) {
                advance();
                return true;
            }
        }
        return false;
    }

    public boolean matchSequence(int... types) {
        if (types == null || types.length == 0)
            return true;
        if (!checkSequence(types))
            return false;
        for (int i = 0; i < types.length; i++) {
            advance();
        }
        return true;
    }

    public boolean match(String lexeme) {
        if (check(lexeme)) {
            advance();
            return true;
        }
        return false;
    }

    public boolean matchAny(String... lexemes) {
        if (lexemes == null || lexemes.length == 0)
            return false;
        for (String l : lexemes) {
            if (check(l)) {
                advance();
                return true;
            }
        }
        return false;
    }

    public boolean matchSequence(String... lexemes) {
        if (lexemes == null || lexemes.length == 0)
            return true;
        if (!checkSequence(lexemes))
            return false;
        for (int i = 0; i < lexemes.length; i++) {
            advance();
        }
        return true;
    }

    public Token expect(int type) {
        Token token = peek();
        if (token == null || token.type != type) {
            throw new IllegalStateException(
                    "Expected " + Token.typeName(type) +
                            " but found " +
                            (token != null ? Token.typeName(token.type) : "EOF"));
        }
        advance();
        return token;
    }

    public Token expect(String lexeme) {
        Token token = peek();
        if (token == null || token.lexeme == null || !token.lexeme.equals(lexeme)) {
            throw new IllegalStateException(
                    "Expected \"" + lexeme +
                            "\" but found \"" +
                            (token != null ? token.lexeme : "EOF") + "\"");
        }
        advance();
        return token;
    }

    public int getHead() {
        return head;
    }

    public int getTail() {
        return tail;
    }

    public int getBufferSize() {
        return buffer.length;
    }

    public void reset() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = null;
        }
        head = 0;
        tail = -1;
        hitEof = false;
        eofIndex = -1;
    }
}