package com.lang.ast;

import com.lang.util.Position;

public class AssignExpr extends Expr {
	public Expr target;
	public Operator operator;
	public Expr value;

	public AssignExpr() {
	}

	public AssignExpr(Expr target, Operator operator, Expr value, Position position) {
		super(position);
		this.target = target;
		this.operator = operator;
		this.value = value;
	}
}