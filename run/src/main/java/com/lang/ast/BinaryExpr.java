package com.lang.ast;

import com.lang.util.Position;

public class BinaryExpr extends Expr {
	public Expr left;
	public Operator operator;
	public Expr right;

	public BinaryExpr() {
	}

	public BinaryExpr(Expr left, Operator operator, Expr right, Position position) {
		super(position);
		this.left = left;
		this.operator = operator;
		this.right = right;
	}

}