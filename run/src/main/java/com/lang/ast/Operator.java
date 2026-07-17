package com.lang.ast;

import com.lang.util.Position;

public class Operator extends Node {
	public final boolean strict;
	public final String lexeme;

	private Operator(boolean strict, String lexeme, Position position) {
		super(position);
		this.strict = strict;
		this.lexeme = lexeme;
	}

	public static Operator of(String lexeme, Position position) {
		return new Operator(false, lexeme, position);
	}

	public static Operator of(boolean strict, String lexeme, Position position) {
		return new Operator(strict, lexeme, position);
	}

	public boolean isCompound() {
		return lexeme.length() > 1;
	}

	public boolean isArithmetic() {
		return lexeme.equals("+") || lexeme.equals("-") ||
			   lexeme.equals("*") || lexeme.equals("/") ||
			   lexeme.equals("%");
	}

	public boolean isComparison() {
		return lexeme.equals("==") || lexeme.equals("!=") ||
			   lexeme.equals("<") || lexeme.equals(">") ||
			   lexeme.equals("<=") || lexeme.equals(">=");
	}

	public boolean isLogical() {
		return lexeme.equals("&&") || lexeme.equals("||");
	}

	public boolean isAssignment() {
		return lexeme.equals("=");
	}

	public boolean isUnary() {
		return lexeme.equals("!") || lexeme.equals("-") || lexeme.equals("+");
	}

	public boolean isStrict() {
		return strict;
	}

	@Override
	public String toString() {
		return strict ? "$" + lexeme : lexeme;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof Operator)) return false;
		Operator that = (Operator) other;
		return strict == that.strict && lexeme.equals(that.lexeme);
	}

	@Override
	public int hashCode() {
		return lexeme.hashCode() ^ (strict ? 1 : 0);
	}
}