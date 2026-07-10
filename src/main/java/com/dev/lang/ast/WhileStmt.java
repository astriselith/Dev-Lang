package com.dev.lang.ast;

import com.dev.lang.util.Position;

public class WhileStmt extends Stmt {
	public final Expr condition;
	public final BlockStmt body;

	public WhileStmt(Expr condition, BlockStmt body, Position position) {
		super(position);
		this.condition = condition;
		this.body = body;
	}
}