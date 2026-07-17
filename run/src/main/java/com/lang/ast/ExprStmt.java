package com.lang.ast;

import com.lang.util.Position;

public class ExprStmt extends Stmt {
	public final Expr expression;

	public ExprStmt(Expr expression, Position position) {
		super(position);
		this.expression = expression;
	}
}