package com.lang.ast;

import com.lang.util.Position;

public class ParamDecl extends Decl {
	public Identifier name;
	public Typed type;
	public Expr init;

	public ParamDecl() {
	}

	public ParamDecl(Identifier name, Typed type, Expr init, Position position) {
		super(position);
		this.name = name;
		this.type = type;
		this.init = init;
	}

	@Override
	public <T> T accept(DeclVisitor<T> visitor) {
		return visitor.visitParamDecl(this);
	}

	
}