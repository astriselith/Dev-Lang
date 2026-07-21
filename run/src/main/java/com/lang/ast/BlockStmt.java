package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class BlockStmt extends Stmt {
	public List<Stmt> statements;

	public BlockStmt() {
	}

	public BlockStmt(List<Stmt> statements, Position position) {
		super(position);
		this.statements = statements;
	}
}