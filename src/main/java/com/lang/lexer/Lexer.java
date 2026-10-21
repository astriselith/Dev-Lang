package com.lang.lexer;

import static com.lang.lexer.LexingErrorCode.*;
import static com.lang.token.Token.*;

import com.lang.token.*;
import com.lang.util.Diagnostic;
import com.lang.util.Position;
import com.lang.codepoint.CodepointStream;
import com.lang.codepoint.Codepoint;
import com.lang.unit.CompilationUnit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Lexer extends TokenStream {

    private final CodepointStream source;
    private final CompilationUnit unit;

    private int current = 0;
    private int currentLineCount = 1;
    private int currentLineStart = 0;

    private int start = 0;
    private int firstLineCount = 1;
    private int firstLineStart = 0;

    private boolean eofReached = false;

    private final Map<String, Integer> keywords = new HashMap<>();
    private final Map<Integer, Integer> symbols = new HashMap<>();

    public Lexer(CodepointStream source, CompilationUnit unit) {
        super(10);

        this.source = source;
        this.unit = unit;

        keywords.put("true", BOOL);
        keywords.put("false", BOOL);
        keywords.put("null", NULL);

        symbols.put((int) '+', PLUS);
        symbols.put((int) '-', MINUS);
        symbols.put((int) '*', STAR);
        symbols.put((int) '/', SLASH);
        symbols.put((int) '%', PERCENT);
        symbols.put((int) '=', EQUALS);
        symbols.put((int) '!', BANG);
        symbols.put((int) '<', LANGLE);
        symbols.put((int) '>', RANGLE);
        symbols.put((int) '&', AMP);
        symbols.put((int) '|', BAR);
        symbols.put((int) '?', QUESTION);
        symbols.put((int) ':', COLON);
        symbols.put((int) '(', LPAREN);
        symbols.put((int) ')', RPAREN);
        symbols.put((int) '{', LBRACE);
        symbols.put((int) '}', RBRACE);
        symbols.put((int) '[', LBRACKET);
        symbols.put((int) ']', RBRACKET);
        symbols.put((int) ',', COMMA);
        symbols.put((int) '.', DOT);
        symbols.put((int) ';', SEMICOLON);
        symbols.put((int) '$', DOLLAR);
        symbols.put((int) '@', AT);
        symbols.put((int) '~', TILDE);
    }

    private void update() {
        int cp = source.advance();
        current++;

        if (cp == '\n') {
            currentLineCount++;
            currentLineStart = current;
        }
    }

    private void update(int n) {
        for (int i = 0; i < n; i++) {
            update();
        }
    }

    private Position position() {
        return new Position(
                firstLineCount, firstLineStart, start,
                currentLineCount, currentLineStart, current);
    }

    private Token undefined(String lexeme) {
        return Token.of(UNDEFINED, lexeme, position());
    }

    private Token eof() {
        return Token.of(EOF, "", new Position(
                currentLineCount, currentLineStart, current,
                currentLineCount, currentLineStart, current));
    }

    @Override
    protected Token fetchNext() {
        if (eofReached) {
            return eof();
        }

        if (source.isAtEnd()) {
            eofReached = true;
            return eof();
        }

        skip();

        if (source.isAtEnd()) {
            eofReached = true;
            return eof();
        }

        int before = current;
        Token token = scan();

        if (current == before) {
            update();
            return undefined(token.lexeme);
        }

        return token;
    }

    private Token scan() {
        int cp = source.peek();

        if (cp == '#') {
            return comment();
        }

        if (symbols.containsKey(cp)) {
            int type = symbols.get(cp);
            update();
            return Token.of(type, Codepoint.toString(cp), position());
        }

        if (Codepoint.isDigit(cp)) {
            return number();
        }

        if (Codepoint.isIdentifierStart(cp)) {
            return identifier();
        }

        if (cp == '"') {
            return string();
        }

        String text = Codepoint.toString(cp);

        unit.addDiagnostic(TAG, Diagnostic.error(position(),
                UNEXPECTED_CHARACTER.format(text)));

        update();
        return undefined(text);
    }

    private void skip() {
        while (!source.isAtEnd()) {
            int cp = source.peek();

            if (!Codepoint.isWhitespace(cp)) {
                break;
            }

            update();
        }

        start = current;
        firstLineCount = currentLineCount;
        firstLineStart = currentLineStart;
    }

    private Token comment() {
        StringBuilder builder = new StringBuilder();

        builder.appendCodePoint('#');
        update();

        boolean closed = false;

        while (!source.isAtEnd()) {
            int cp = source.peek();

            if (cp == '#') {
                builder.appendCodePoint('#');
                update();
                closed = true;
                break;
            }

            builder.appendCodePoint(cp);
            update();
        }

        String text = builder.toString();
        Position pos = position();

        if (!closed) {
            unit.addDiagnostic(TAG, Diagnostic.error(pos,
                    UNTERMINATED_COMMENT.format()));
        }

        return Token.of(COMMENT, text, pos);
    }

    private Token identifier() {
        StringBuilder builder = new StringBuilder();

        while (!source.isAtEnd()) {
            int cp = source.peek();

            if (!Codepoint.isIdentifier(cp)) {
                break;
            }

            builder.appendCodePoint(cp);
            update();
        }

        String text = builder.toString();

        if (keywords.containsKey(text)) {
            int type = keywords.get(text);
            return Token.of(type, text, position());
        }

        return Token.of(IDENTIFIER, text, position());
    }

    private Token number() {
        StringBuilder builder = new StringBuilder();

        while (!source.isAtEnd()) {
            int cp = source.peek();

            if (Codepoint.isDigit(cp) || cp == '_' || cp == '.' || cp == 'e' || cp == 'E') {
                builder.appendCodePoint(cp);
                update();
            } else {
                break;
            }
        }

        String text = builder.toString();
        String clean = text.replace("_", "");

        if (clean.matches("^[0-9]+$")) {
            return Token.of(INT, text, position());
        }

        if (clean.matches("^[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?$")) {
            return Token.of(FLOAT, text, position());
        }

        unit.addDiagnostic(TAG, Diagnostic.error(position(),
                INVALID_NUMBER.format(text)));

        return undefined(text);
    }

    private Token string() {
        StringBuilder lexeme = new StringBuilder();
        lexeme.append('"');
        update();

        while (!source.isAtEnd()) {
            int cp = source.peek();

            if (cp == '"') {
                lexeme.append('"');
                update();
                return Token.of(STRING, lexeme.toString(), position());
            }

            if (cp == '\\') {
                lexeme.append('\\');
                update();

                if (source.isAtEnd()) {
                    break;
                }

                int next = source.peek();

                switch (next) {
                    case 'n':
                    case 'r':
                    case 't':
                    case '\\':
                    case '"':
                        lexeme.appendCodePoint(next);
                        update();
                        break;
                    default:
                        lexeme.appendCodePoint(next);
                        update();
                        unit.addDiagnostic(TAG, Diagnostic.error(position(),
                                UNKNOWN_ESCAPE_SEQUENCE.format(Codepoint.toString(next))));
                        return undefined(lexeme.toString());
                }

                continue;
            }

            lexeme.appendCodePoint(cp);
            update();
        }

        unit.addDiagnostic(TAG, Diagnostic.error(position(),
                UNTERMINATED_STRING.format()));
        return undefined(lexeme.toString());
    }

    public CompilationUnit getCompilationUnit() {
        return unit;
    }

    @Override
    public void close() throws IOException {
        source.close();
    }
}