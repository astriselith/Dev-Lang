package com.lang.ast;

import com.lang.util.Position;

public class MemberAccessExpr extends Expr {
	public final Expr object;
	public final String name;

	public MemberAccessExpr(Expr object, String name, Position position) {
		super(position);
		this.object = object;
		this.name = name;
	}
}