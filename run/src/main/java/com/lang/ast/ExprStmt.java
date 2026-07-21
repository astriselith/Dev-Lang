package com.lang.ast;

import com.lang.util.Position;

public class ExprStmt extends Stmt {
	public Expr expression;

	public ExprStmt() {
	}

	public ExprStmt(Expr expression, Position position) {
		super(position);
		this.expression = expression;
	}
}