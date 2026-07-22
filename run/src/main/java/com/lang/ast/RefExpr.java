package com.lang.ast;

import com.lang.util.Position;

public class RefExpr extends Expr {
	public String name;

	public RefExpr() {
	}

	public RefExpr(String name, Position position) {
		super(position);
		this.name = name;
	}
}