package com.lang.ast;

import com.lang.util.Position;

public class TernaryExpr extends Expr {
	public Expr condition;
	public Expr thenExpr;
	public Expr elseExpr;

	public TernaryExpr() {
	}

	public TernaryExpr(Expr condition, Expr thenExpr, Expr elseExpr, Position position) {
		super(position);
		this.condition = condition;
		this.thenExpr = thenExpr;
		this.elseExpr = elseExpr;
	}

}