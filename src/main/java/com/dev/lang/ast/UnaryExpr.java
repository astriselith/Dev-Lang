package com.dev.lang.ast;

import com.dev.lang.util.Position;

public class UnaryExpr extends Expr {
	public final Operator operator;
	public final Expr operand;

	public UnaryExpr(Operator operator, Expr operand, Position position) {
		super(position);
		this.operator = operator;
		this.operand = operand;
	}
}