package com.lang.ast;

import com.lang.util.Position;

public class IfStmt extends Stmt {
	public Expr condition;
	public BlockStmt thenBranch;
	public BlockStmt elseBranch;

	public IfStmt() {
	}

	public IfStmt(Expr condition, BlockStmt thenBranch, BlockStmt elseBranch, Position position) {
		super(position);
		this.condition = condition;
		this.thenBranch = thenBranch;
		this.elseBranch = elseBranch;
	}

}
