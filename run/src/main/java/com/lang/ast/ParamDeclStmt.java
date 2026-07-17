package com.lang.ast;

import com.lang.util.Position;

public class ParamDeclStmt extends Stmt {
	public final String name;
	public final Typed type;

	public ParamDeclStmt(String name, Typed type, Position position) {
		super(position);
		this.name = name;
		this.type = type;
	}

	public ParamDeclStmt(String name, Position position) {
		this(name, null, position);
	}

	public boolean hasType() {
		return type != null;
	}

	@Override
	public String toString() {
		if (type != null) {
			return name + ": " + type.getName();
		}
		return name;
	}
}