package com.lang.ast;

import com.lang.util.Position;

public class Operator extends Node {
	public String lexeme;

	public Operator() {
	}

	public Operator(String lexeme, Position position) {
		super(position);
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

	@Override
	public String toString() {
		return lexeme;
	}

}