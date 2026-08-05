package com.lang.ast;

import com.lang.util.Position;

public class VarDecl extends MemberDecl {

	public Typed type;
	public Expr value;

	public VarDecl() {
	}

	public VarDecl(Identifier name, Typed type, Expr value, Position position) {
		super(name, position);
		this.type = type;
		this.value = value;
	}
}