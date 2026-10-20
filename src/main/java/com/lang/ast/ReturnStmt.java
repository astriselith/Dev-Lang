package com.lang.ast;

import com.lang.util.Position;

public class ReturnStmt extends Stmt {
	public Expr value;

	public ReturnStmt() {
	}

	public ReturnStmt(Expr value, Position position) {
		super(position);
		this.value = value;
	}
}