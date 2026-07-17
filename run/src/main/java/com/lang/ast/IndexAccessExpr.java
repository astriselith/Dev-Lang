package com.lang.ast;

import com.lang.util.Position;

public class IndexAccessExpr extends Expr {
	public final Expr target;
	public final Expr index;

	public IndexAccessExpr(Expr target, Expr index, Position position) {
		super(position);
		this.target = target;
		this.index = index;
	}
}