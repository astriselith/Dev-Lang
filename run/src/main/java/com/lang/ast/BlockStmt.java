package com.lang.ast;

import com.lang.util.Position;
import java.util.List;

public class BlockStmt extends Stmt {
	public final List<Stmt> statements;

	public BlockStmt(List<Stmt> statements, Position position) {
		super(position);
		this.statements = statements != null ? statements : List.of();
	}
}