package com.dev.lang.ast;

import com.dev.lang.util.Position;

public class RefExpr extends Expr {
	public final String name;

	public RefExpr(String name, Position position) {
		super(position);
		this.name = name;
	}
}