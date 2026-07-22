package com.lang.ast;

import com.lang.util.Position;

public class ParamDeclStmt extends Stmt {
	public String name;
	public Typed type;

	public ParamDeclStmt() {
	}

	public ParamDeclStmt(String name, Typed type, Position position) {
		super(position);
		this.name = name;
		this.type = type;
	}
}