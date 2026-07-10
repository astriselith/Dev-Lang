package com.dev.lang.ast;

import com.dev.lang.util.Position;

public class AssignExpr extends Expr {
	public final Expr target;
	public final Operator operator;
	public final Expr value;

	public AssignExpr(Expr target, Operator operator, Expr value, Position position) {
		super(position);
		this.target = target;
		this.operator = operator;
		this.value = value;
	}
}