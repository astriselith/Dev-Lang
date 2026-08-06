package com.lang.ast;

import com.lang.util.Position;

public class ParamDecl extends Decl {
	public Identifier name;
	public Typed type;
	public Expr value;

	public ParamDecl() {
	}

	public ParamDecl(Identifier name, Typed type, Expr value, Position position) {
		super(position);
		this.name = name;
		this.type = type;
		this.value = value;
	}
}