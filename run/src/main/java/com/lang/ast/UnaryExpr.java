package com.lang.ast;

import com.lang.util.Position;

public class UnaryExpr extends Expr {
	public Operator operator;
	public Expr operand;

	public UnaryExpr() {
	}

	public UnaryExpr(Operator operator, Expr operand, Position position) {
		super(position);
		this.operator = operator;
		this.operand = operand;
	}

}