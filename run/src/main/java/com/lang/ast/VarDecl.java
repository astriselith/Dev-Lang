package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class VarDecl extends MemberDecl {

	public Typed type;
	public Expr value;

	public VarDecl() {
	}

	public VarDecl(List<Meta> metas, Identifier name, Typed type, Expr value, Position position) {
		super(metas, name, position);
		this.type = type;
		this.value = value;
	}
}