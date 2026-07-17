package com.lang.ast;

import com.lang.util.Position;

public class RefExpr extends Expr {
	public final String name;

	public RefExpr(String name, Position position) {
		super(position);
		this.name = name;
	}
}