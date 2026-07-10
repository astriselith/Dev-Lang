package com.dev.lang.ast;

import com.dev.lang.util.Position;

public class BinaryExpr extends Expr {
	public final Expr left;
	public final Operator operator;
	public final Expr right;

	public BinaryExpr(Expr left, Operator operator, Expr right, Position position) {
		super(position);
		this.left = left;
		this.operator = operator;
		this.right = right;
	}
}