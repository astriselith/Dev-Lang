package com.lang.ast;

import com.lang.util.Position;

public class WhileStmt extends Stmt {
	public Expr condition;
	public Stmt body;

	public WhileStmt() {
	}

	public WhileStmt(Expr condition, Stmt body, Position position) {
		super(position);
		this.condition = condition;
		this.body = body;
	}
}