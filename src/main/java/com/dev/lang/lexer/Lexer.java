package com.dev.lang.lexer;

import static com.dev.lang.lexer.LexingErrorCode.*;
import static com.dev.lang.token.Type.*;

import com.dev.lang.token.*;
import com.dev.lang.util.Pair;
import com.dev.lang.util.Position;
import com.dev.lang.codepoint.CodepointStream;
import com.dev.lang.codepoint.Codepoint;
import com.dev.lang.unit.CompilationException;
import com.dev.lang.unit.CompilationUnit;

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
		this(source, null, true);
	}

	public Lexer(CodepointStream source, CompilationUnit unit) {
		this(source, unit, true);
	}

	public Lexer(CodepointStream source, CompilationUnit unit, boolean includeKeyword) {
		super(10, 10, 4);
		this.source = source;
		this.unit = unit != null ? unit : new CompilationUnit();

		if (includeKeyword) {
			addKeyword("public", PUBLIC);
			addKeyword("private", PRIVATE);
			addKeyword("shared", SHARED);
			addKeyword("readonly", READONLY);
			addKeyword("var", VAR);
			addKeyword("fun", FUN);
			addKeyword("class", CLASS);
			addKeyword("trait", TRAIT);
			addKeyword("if", IF);
			addKeyword("else", ELSE);
			addKeyword("while", WHILE);
			addKeyword("break", BREAK);
			addKeyword("continue", CONTINUE);
			addKeyword("return", RETURN);
		}

		addKeyword("true", BOOLEAN);
		addKeyword("false", BOOLEAN);
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

				skipWhitespace();

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
								  e
							  ));
			}
		}
	}

	private void skipWhitespace() {
		while (!source.isAtEnd()) {
			int cp = source.peek();

			if (Codepoint.isWhitespace(cp)) {
				update();
				continue;
			}

			if (cp == '/') {
				int next = source.peekNext();
				if (next == '/') {
					update(2);
					while (!source.isAtEnd() && source.peek() != '\n') {
						update();
					}
					continue;
				} else if (next == '*') {
					update(2);
					boolean closed = false;
					while (!source.isAtEnd()) {
						if (source.peek() == '*' && source.peekNext() == '/') {
							update(2);
							closed = true;
							break;
						}
						update();
					}
					if (!closed) {
						throw unit.error(
							TAG,
							UNTERMINATED_COMMENT.format(),
							new Position(line, lineStart, start, current)
						);
					}
					continue;
				}
			}

			break;
		}
	}

	private Token scan() {
		int cp = source.peek();

		if (cp == -1) {
			return Token.eof(line, lineStart, current, current);
		}

		Pair<Type, String> symbol = symbols.get(cp);
		if (symbol != null) {
			update();
			return Token.of(symbol.getFirst(), symbol.getSecond(), null, line, lineStart, start, current);
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
			new Position(line, lineStart, start, current)
		);
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

			if (type == BOOLEAN) {
				return Token.of(type, lexeme, Boolean.parseBoolean(text), line, lineStart, start, current);
			} else if (type == NULL) {
				return Token.of(type, lexeme, null, line, lineStart, start, current);
			} else {
				return Token.of(type, lexeme, null, line, lineStart, start, current);
			}
		} else {
			return Token.of(IDENTIFIER, text, null, line, lineStart, start, current);
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
			try {
				long value = Long.parseLong(clean);
				return Token.of(INT, text, value, line, lineStart, start, current);
			} catch (NumberFormatException e) {
				unit.addError(unit.error(
								  TAG,
								  INVALID_NUMBER.format(text),
								  new Position(line, lineStart, start, current),
								  e
							  ));
				return Token.of(UNDEFINED, text, null, line, lineStart, start, current);
			}
		}

		if (clean.matches("^[0-9]+(\\.[0-9]+)?([eE][+-]?[0-9]+)?$")) {
			try {
				double value = Double.parseDouble(clean);
				return Token.of(FLOAT, text, value, line, lineStart, start, current);
			} catch (NumberFormatException e) {
				unit.addError(unit.error(
								  TAG,
								  INVALID_NUMBER.format(text),
								  new Position(line, lineStart, start, current),
								  e
							  ));
				return Token.of(UNDEFINED, text, null, line, lineStart, start, current);
			}
		}

		unit.addError(unit.error(
						  TAG,
						  INVALID_NUMBER.format(text),
						  new Position(line, lineStart, start, current)
					  ));
		return Token.of(UNDEFINED, text, null, line, lineStart, start, current);
	}

	private Token quoted() {
		int quote = source.peek();
		StringBuilder builder = new StringBuilder();
		StringBuilder lexemeBuilder = new StringBuilder();

		lexemeBuilder.appendCodePoint(quote);
		update();

		while (!source.isAtEnd()) {
			int cp = source.peek();
			if (cp == quote) break;

			if (cp == '\n') {
				if (quote == '"') {
					builder.appendCodePoint(cp);
					lexemeBuilder.appendCodePoint(cp);
					update();
				} else {
					throw unit.error(
						TAG,
						UNTERMINATED_CHARACTER.format(),
						new Position(line, lineStart, start, current)
					);
				}
			} else if (cp == '\\') {
				lexemeBuilder.appendCodePoint(cp);
				update();
				if (source.isAtEnd()) break;

				int escaped;
				int next = source.peek();
				lexemeBuilder.appendCodePoint(next);

				switch (next) {
				case 'n':
					escaped = '\n';
					break;
				case 'r':
					escaped = '\r';
					break;
				case 't':
					escaped = '\t';
					break;
				case '\\':
					escaped = '\\';
					break;
				case '"':
					escaped = '"';
					break;
				case '\'':
					escaped = '\'';
					break;
				default:
					throw unit.error(
						TAG,
						UNKNOWN_ESCAPE_SEQUENCE.format(String.valueOf((char) next)),
						new Position(line, lineStart, start, current)
					);
				}
				builder.appendCodePoint(escaped);
				update();
			} else {
				builder.appendCodePoint(cp);
				lexemeBuilder.appendCodePoint(cp);
				update();
			}
		}

		if (source.isAtEnd()) {
			throw unit.error(
				TAG,
				quote == '"' ? UNTERMINATED_STRING.format() : UNTERMINATED_CHARACTER.format(),
				new Position(line, lineStart, start, current)
			);
		}

		lexemeBuilder.appendCodePoint(quote);
		update();

		String value = builder.toString();
		String lexeme = lexemeBuilder.toString();

		if (quote == '\'') {
			char charValue;
			if (value.length() == 0) {
				charValue = '\0';
			} else if (value.length() == 1) {
				charValue = value.charAt(0);
			} else {
				throw unit.error(
					TAG,
					INVALID_CHARACTER_LITERAL.format(),
					new Position(line, lineStart, start, current)
				);
			}
			return Token.of(CHAR, lexeme, charValue, line, lineStart, start, current);
		}

		return Token.of(STRING, lexeme, value, line, lineStart, start, current);
	}

	public CompilationUnit getCompilationUnit() {
		return unit;
	}

	public void close() throws IOException {
		source.close();
	}
}