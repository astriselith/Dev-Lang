package com.lang.ast;

import com.lang.util.Position;

public class Operator extends Node {
	public final boolean strict;
	public final String lexeme;

	public Operator(boolean strict, String lexeme, Position position) {
		super(position);
		this.strict = strict;
		this.lexeme = lexeme;
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
}