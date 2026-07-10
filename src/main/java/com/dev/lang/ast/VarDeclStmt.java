package com.dev.lang.ast;

import com.dev.lang.util.Position;

public class VarDeclStmt extends Stmt {
	public final Modifier modifier;
	public final String name;
	public final Typed type;
	public final Expr value;

	public VarDeclStmt(Modifier modifier, String name, Typed type, Expr value, Position position) {
		super(position);
		this.modifier = modifier != null ? modifier : new Modifier(position);
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