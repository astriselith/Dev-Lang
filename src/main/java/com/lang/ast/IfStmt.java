package com.lang.ast;

import com.lang.util.Position;

public class IfStmt extends Stmt {
	public Expr condition;
	public Stmt thenBranch;
	public Stmt elseBranch;

	public IfStmt() {
	}

	public IfStmt(Expr condition, Stmt thenBranch, Stmt elseBranch, Position position) {
		super(position);
		this.condition = condition;
		this.thenBranch = thenBranch;
		this.elseBranch = elseBranch;
	}
}
