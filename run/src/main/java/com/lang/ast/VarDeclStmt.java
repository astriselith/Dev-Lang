package com.lang.ast;

import com.lang.util.Position;

public class VarDeclStmt extends MemberDeclStmt {

	public Typed type;
	public Expr value;

	public VarDeclStmt() {
	}

	public VarDeclStmt(Identifier name, Typed type, Expr value, Position position) {
		super(name, position);
		this.type = type;
		this.value = value;
	}
}