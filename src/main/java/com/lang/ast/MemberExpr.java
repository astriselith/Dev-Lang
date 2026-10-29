package com.lang.ast;

import com.lang.util.Position;

public class MemberExpr extends Expr {
	public Expr object;
	public Identifier name;

	public MemberExpr() {
	}

	public MemberExpr(Expr object, Identifier name, Position position) {
		super(position);
		this.object = object;
		this.name = name;
	}
}