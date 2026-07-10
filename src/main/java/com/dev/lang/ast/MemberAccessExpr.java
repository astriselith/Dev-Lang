package com.dev.lang.ast;

import com.dev.lang.util.Position;

public class MemberAccessExpr extends Expr {
	public final Expr object;
	public final String name;

	public MemberAccessExpr(Expr object, String name, Position position) {
		super(position);
		this.object = object;
		this.name = name;
	}
}