package com.lang.ast;

import com.lang.util.Position;

public class IndexAccessExpr extends Expr {
	public Expr target;
	public Expr index;

	public IndexAccessExpr() {
	}

	public IndexAccessExpr(Expr target, Expr index, Position position) {
		super(position);
		this.target = target;
		this.index = index;
	}

}