package com.lang.ast;

import com.lang.util.Position;

public class VarDeclStmt extends Stmt {
	public final String name;
	public final Typed type;
	public final Expr value;

	public VarDeclStmt(String name, Typed type, Expr value, Position position) {
		super(position);
		this.name = name;
		this.type = type;
		this.value = value;
	}

	public boolean hasType() {
		return type != null;
	}

	public boolean hasValue() {
		return value != null;
	}
}