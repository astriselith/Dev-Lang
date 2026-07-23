package com.lang.ast;

import com.lang.util.Position;

public class RefExpr extends Expr {
	public Identifier name;

	public RefExpr() {
	}

	public RefExpr(Identifier name, Position position) {
		super(position);
		this.name = name;
	}
}