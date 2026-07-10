package com.dev.lang.ast;

import com.dev.lang.util.Position;

public class IfStmt extends Stmt {
	public final Expr condition;
	public final BlockStmt thenBranch;
	public final BlockStmt elseBranch;

	public IfStmt(Expr condition, BlockStmt thenBranch, BlockStmt elseBranch, Position position) {
		super(position);
		this.condition = condition;
		this.thenBranch = thenBranch;
		this.elseBranch = elseBranch;
	}
}