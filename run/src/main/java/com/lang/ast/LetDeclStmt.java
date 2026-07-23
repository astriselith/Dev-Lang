package com.lang.ast;

import com.lang.util.Position;

public class LetDeclStmt extends Stmt {
	public Identifier name;
	public Typed type;
	public Expr value;

	public LetDeclStmt() {
	}

	public LetDeclStmt(Identifier name, Typed type, Expr value, Position position) {
		super(position);
		this.name = name;
		this.type = type;
		this.value = value;
	}
}