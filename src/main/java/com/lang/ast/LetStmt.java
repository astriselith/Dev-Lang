package com.lang.ast;

import com.lang.util.Position;

public class LetStmt extends Stmt {
	public Identifier name;
	public Typed type;
	public Expr value;

	public LetStmt() {
	}

	public LetStmt(Identifier name, Typed type, Expr value, Position position) {
		super(position);
		this.name = name;
		this.type = type;
		this.value = value;
	}
}