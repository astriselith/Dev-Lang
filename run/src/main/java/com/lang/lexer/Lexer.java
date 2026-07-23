package com.lang.lexer;

import static com.lang.lexer.LexingErrorCode.*;
import static com.lang.token.Type.*;

import com.lang.token.*;
import com.lang.util.Pair;
import com.lang.util.Position;
import com.lang.codepoint.CodepointStream;
import com.lang.codepoint.Codepoint;
import com.lang.unit.CompilationException;
import com.lang.unit.CompilationUnit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Lexer extends TokenStream {
	private final CodepointStream source;
	private final CompilationUnit unit;
	private int current = 0;
	private int line = 1;
	private int lineStart = 0;
	private int start = 0;
	private boolean eofReached = false;

	private final Map<String, Pair<Type, String>> keywords = new HashMap<>();
	private final Map<Integer, Pair<Type, String>> symbols = new HashMap<>();

	public Lexer(CodepointStream source) {
		this(source, null);
	}

	public Lexer(CodepointStream source, CompilationUnit unit) {
		super(10);
		this.source = source;
		this.unit = unit != null ? unit : new CompilationUnit();

		addKeyword("true", BOOL);
		addKeyword("false", BOOL);
		addKeyword("null", NULL);

		addSymbol('+', PLUS);
		addSymbol('-', MINUS);
		addSymbol('*', STAR);
		addSymbol('/', SLASH);
		addSymbol('%', PERCENT);
		addSymbol('=', EQUALS);
		addSymbol('!', BANG);
		addSymbol('<', LANGLE);
		addSymbol('>', RANGLE);
		addSymbol('&', AMP);
		addSymbol('|', BAR);
		addSymbol('?', QUESTION);
		addSymbol(':', COLON);
		addSymbol('(', LPAREN);
		addSymbol(')', RPAREN);
		addSymbol('{', LBRACE);
		addSymbol('}', RBRACE);
		addSymbol('[', LBRACKET);
		addSymbol(']', RBRACKET);
		addSymbol(',', COMMA);
		addSymbol('.', DOT);
		addSymbol(';', SEMICOLON);
		addSymbol('$', DOLLAR);
		addSymbol('@', AT);
	}

	private void addKeyword(String keyword, Type token) {
		keywords.put(keyword, Pair.of(token, keyword));
	}

	private void addSymbol(int cp, Type token) {
		symbols.put(cp, Pair.of(token, Codepoint.toString(cp)));
	}

	private void update() {
		int cp = source.advance();
		current++;
		if (cp == '\n') {
			line++;
			lineStart = current;
		}
	}

	private void update(int n) {
		for (int i = 0; i < n; i++) {
			update();
		}
	}

	@Override
	protected Token fetchNext() {
		while (true) {
			try {
				if (eofReached) {
					return Token.eof(line, lineStart, current, current);
				}

				if (source.isAtEnd()) {
					eofReached = true;
					return Token.eof(line, lineStart, current, current);
				}

				start = current;
				return scan();
			} catch (CompilationException e) {
				unit.addError(e);
			} catch (Exception e) {
				unit.addError(unit.error(
						TAG,
						e.getMessage(),
						new Position(line, lineStart, current, current),
						e));
			}
		}
	}

	private Token scan() {
		int cp = source.peek();

		if (Codepoint.isWhitespace(cp)) {
			update();
			return scan();
		}

		if (cp == '/') {
			StringBuilder comment = new StringBuilder();
			int next = source.peekNext();
			if (next == '/') {
				update(2);
				while (!source.isAtEnd() && source.peek() != '\n') {
					comment.appendCodePoint(source.peek());
					update();
				}
				return Token.of(COMMENT, comment.toString(), line, lineStart, start, current);
			} else if (next == '*') {
				update(2);
				boolean closed = false;
				while (!source.isAtEnd()) {
					if (source.peek() == '*' && source.peekNext() == '/') {
						update(2);
						closed = true;
						break;
					}
					comment.appendCodePoint(source.peek());
					update();
				}
				if (!closed) {
					throw unit.error(
							TAG,
							UNTERMINATED_COMMENT.format(),
							new Position(line, lineStart, start, current));
				}
				return Token.of(MULTILINE_COMMENT, comment.toString(), line, lineStart, start, current);
			}
		}
		if (cp == -1) {
			return Token.eof(line, lineStart, current, current);
		}

		Pair<Type, String> symbol = symbols.get(cp);
		if (symbol != null) {
			update();
			return Token.of(symbol.getFirst(), symbol.getSecond(), line, lineStart, start, current);
		}

		if (Codepoint.isDigit(cp)) {
			return number();
		}

		if (Codepoint.isLetter(cp) || cp == '_') {
			return identifier();
		}

		if (cp == '"' || cp == '\'') {
			return quoted();
		}

		throw unit.error(
				TAG,
				UNEXPECTED_CHARACTER.format(String.valueOf((char) cp)),
				new Position(line, lineStart, start, current));
	}

	private Token identifier() {
		StringBuilder builder = new StringBuilder();

		while (!source.isAtEnd()) {
			int cp = source.peek();
			if (Codepoint.isIdentifier(cp)) {
				builder.appendCodePoint(cp);
				update();
			} else {
				break;
			}
		}

		String text = builder.toString();
		Pair<Type, String> keyword = keywords.get(text);

		if (keyword != null) {
			Type type = keyword.getFirst();
			String lexeme = keyword.getSecond();

			if (type == BOOL) {
				return Token.of(type, lexeme, line, lineStart, start, current);
			} else if (type == NULL) {
				return Token.of(type, lexeme, line, lineStart, start, current);
			} else {
				return Token.of(type, lexeme, line, lineStart, start, current);
			}
		} else {
			return Token.of(IDENTIFIER, text, line, lineStart, start, current);
		}
	}

	private Token number() {
		StringBuilder sb = new StringBuilder();

		while (!source.isAtEnd()) {
			int cp = source.peek();

			if (Codepoint.isDigit(cp) || cp == '_' || cp == '.' || cp == 'e' || cp == 'E') {
				sb.appendCodePoint(cp);
				update();
			} else {
				break;
			}
		}

		String text = sb.toString();
		String clean = text.replace("_", "");
		if (clean.matches("^[0-9]+$")) {
			return Token.of(INT, text, line, lineStart, start, current);
		}

		if (clean.matches("^[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?$")) {
			return Token.of(FLOAT, text, line, lineStart, start, current);

		}

		unit.addError(unit.error(
				TAG,
				INVALID_NUMBER.format(text),
				new Position(line, lineStart, start, current)));
		return Token.of(UNDEFINED, text, line, lineStart, start, current);
	}

	private Token quoted() {
		int quote = source.peek();
		StringBuilder builder = new StringBuilder();
		StringBuilder lexemeBuilder = new StringBuilder();

		lexemeBuilder.appendCodePoint(quote);
		update();

		while (!source.isAtEnd()) {
			int cp = source.peek();
			if (cp == quote)
				break;

			if (cp == '\n') {
				if (quote == '"') {
					builder.appendCodePoint(cp);
					lexemeBuilder.appendCodePoint(cp);
					update();
				} else {
					throw unit.error(
							TAG,
							UNTERMINATED_CHARACTER_OR_STRING.format(),
							new Position(line, lineStart, start, current));
				}
			} else if (cp == '\\') {
				lexemeBuilder.appendCodePoint(cp);
				update();

				if (source.isAtEnd())
					break;

				int next = source.peek();

				switch (next) {
					case 'n':
					case 'r':
					case 't':
					case '\\':
					case '"':
					case '\'':
						lexemeBuilder.appendCodePoint(next);
						update();
						break;
					default:
						throw unit.error(
								TAG,
								UNKNOWN_ESCAPE_SEQUENCE.format(String.valueOf((char) next)),
								new Position(line, lineStart, start, current));
				}
			} else {
				lexemeBuilder.appendCodePoint(cp);
				update();
			}
		}

		if (source.isAtEnd()) {
			throw unit.error(
					TAG,
					UNTERMINATED_CHARACTER_OR_STRING.format(),
					new Position(line, lineStart, start, current));
		}

		lexemeBuilder.appendCodePoint(quote);
		update();

		String value = builder.toString();
		String lexeme = lexemeBuilder.toString();

		if (quote == '\'') {
			if (!(value.length() == 0) && !(value.length() == 1)) {
				throw unit.error(
						TAG,
						INVALID_CHARACTER_LITERAL.format(),
						new Position(line, lineStart, start, current));
			}
			return Token.of(CHAR, lexeme, line, lineStart, start, current);
		}

		return Token.of(STRING, lexeme, line, lineStart, start, current);
	}

	public CompilationUnit getCompilationUnit() {
		return unit;
	}

	public void close() throws IOException {
		source.close();
	}
}