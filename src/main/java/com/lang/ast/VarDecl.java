package com.lang.ast;

import com.lang.util.Position;

public class VarDecl extends MemberDecl {
	public Expr value;

	public VarDecl() {
	}

	public VarDecl(Identifier name, Typed type, Expr value, Position position) {
		super(name, type, position);
		this.value = value;
	}

	@Override
	public <T> T accept(DeclVisitor<T> visitor) {
		return visitor.visitVarDecl(this);
	}
}