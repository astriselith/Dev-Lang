package com.dev.lang.ast;

import com.dev.lang.util.Position;

public class ReturnStmt extends Stmt {
	public final Expr value;

	public ReturnStmt(Expr value, Position position) {
		super(position);
		this.value = value;
	}
}