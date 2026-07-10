package com.dev.lang.ast;

import com.dev.lang.util.Position;

public class ExprStmt extends Stmt {
	public final Expr expression;

	public ExprStmt(Expr expression, Position position) {
		super(position);
		this.expression = expression;
	}
}