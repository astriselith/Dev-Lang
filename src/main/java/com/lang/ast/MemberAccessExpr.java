package com.lang.ast;

import com.lang.util.Position;

public class MemberAccessExpr extends Expr {
	public Expr object;
	public Identifier name;

	public MemberAccessExpr() {
	}

	public MemberAccessExpr(Expr object, Identifier name, Position position) {
		super(position);
		this.object = object;
		this.name = name;
	}
}