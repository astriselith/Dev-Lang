package com.lang.ast;

import com.lang.util.Position;

public class ReturnStmt extends Stmt {
	public final Expr value;

	public ReturnStmt(Expr value, Position position) {
		super(position);
		this.value = value;
	}
}