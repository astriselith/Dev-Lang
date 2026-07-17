package com.lang.token;

import com.lang.util.Position;
import com.lang.util.Positioned;

public class Token implements Positioned {
	public final Type type;
	public final String lexeme;
	public final Object literal;
	public final Position position;

	private Token(Type type, String lexeme, Object literal, Position position) {
		this.type = type;
		this.lexeme = lexeme;
		this.literal = literal;
		this.position = position;
	}

	public static Token of(Type type, String lexeme, Object literal, int line, int lineStart, int start, int end) {
		return new Token(type, lexeme, literal, new Position(line, lineStart, start, end));
	}

	public static Token of(Type type, String lexeme, int line, int lineStart, int start, int end) {
		return of(type, lexeme, null, line, lineStart, start, end);
	}

	public static Token eof(int line, int lineStart, int start, int end) {
		return of(Type.EOF, "", null, line, lineStart, start, end);
	}

	public int getLine() {
		return position.getLine();
	}

	public int getLineStart() {
		return position.getLineStart();
	}

	public int getStart() {
		return position.getStart();
	}

	public int getEnd() {
		return position.getEnd();
	}

	@Override
	public Position getPosition() {
		return position;
	}

	public boolean typeEquals(Type... types) {
		for (Type t : types) {
			if (type == t) {
				return true;
			}
		}
		return false;
	}

	public boolean isComment() {
		return typeEquals(Type.COMMENT, Type.MULTILINE_COMMENT);
	}

	public boolean isOperator() {
		return typeEquals(
				   Type.PLUS, Type.MINUS, Type.STAR, Type.SLASH, Type.PERCENT,
				   Type.AMP, Type.BAR, Type.BANG, Type.EQUALS,
				   Type.LANGLE, Type.RANGLE, Type.QUESTION, Type.COLON
			   );
	}

	public boolean isComparison() {
		return typeEquals(Type.LANGLE, Type.RANGLE);
	}

	public boolean isLogical() {
		return typeEquals(Type.AMP, Type.BAR);
	}

	public boolean isBitwise() {
		return typeEquals(Type.AMP, Type.BAR);
	}

	public boolean isArithmetic() {
		return typeEquals(Type.PLUS, Type.MINUS, Type.STAR, Type.SLASH, Type.PERCENT);
	}

	public boolean isLiteral() {
		return typeEquals(
				   Type.NULL, Type.BOOLEAN, Type.CHAR,
				   Type.INT, Type.FLOAT, Type.STRING
			   );
	}

	public boolean isPunctuation() {
		return typeEquals(
				   Type.LPAREN, Type.RPAREN,
				   Type.LBRACE, Type.RBRACE,
				   Type.LBRACKET, Type.RBRACKET,
				   Type.COMMA, Type.DOT, Type.SEMICOLON, Type.COLON
			   );
	}

	public boolean isClosing() {
		return typeEquals(
				   Type.RPAREN, Type.RBRACE, Type.RBRACKET,
				   Type.SEMICOLON, Type.EOF
			   );
	}

	public boolean isOpening() {
		return typeEquals(Type.LPAREN, Type.LBRACE, Type.LBRACKET);
	}

	public boolean isAssignment() {
		return typeEquals(Type.EQUALS);
	}

	public boolean isUnary() {
		return typeEquals(Type.BANG, Type.MINUS, Type.PLUS);
	}

	public boolean isUndefined() {
		return type == Type.UNDEFINED;
	}

	public boolean isEof() {
		return type == Type.EOF;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null) return false;

		if (other instanceof Token) {
			Token that = (Token) other;
			return this.type == that.type;
		}

		if (other instanceof Type) {
			return this.type == (Type) other;
		}

		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(type);
		sb.append(" '").append(lexeme).append("'");
		if (literal != null) {
			sb.append(" = ").append(literal);
		}
		sb.append(" ").append(position);
		return sb.toString();
	}
}