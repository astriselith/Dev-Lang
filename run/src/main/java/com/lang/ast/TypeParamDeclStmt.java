package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class TypeParamDeclStmt extends Stmt {
	public String name;
	public List<Typed> superclasses;

	public TypeParamDeclStmt() {
	}

	public TypeParamDeclStmt(String name, List<Typed> superclasses, Position position) {
		super(position);
		this.name = name;
		this.superclasses = superclasses;
	}
}