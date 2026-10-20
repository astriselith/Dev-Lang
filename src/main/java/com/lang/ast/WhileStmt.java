package com.lang.ast;

import com.lang.util.Position;

public class WhileStmt extends Stmt {
	public Expr condition;
	public BlockStmt body;

	public WhileStmt() {
	}

	public WhileStmt(Expr condition, BlockStmt body, Position position) {
		super(position);
		this.condition = condition;
		this.body = body;
	}
}