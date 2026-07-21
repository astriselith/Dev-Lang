package com.lang.ast;

import com.lang.util.Position;

public class MemberAccessExpr extends Expr {
	public Expr object;
	public String name;

	public MemberAccessExpr() {
	}

	public MemberAccessExpr(Expr object, String name, Position position) {
		super(position);
		this.object = object;
		this.name = name;
	}

}