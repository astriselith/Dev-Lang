package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class TypeParamDeclStmt extends Stmt {
	public String name;
	public Typed superclass;
	public List<Typed> supertraits;

	public TypeParamDeclStmt() {
	}

	public TypeParamDeclStmt(String name, Typed superclass, List<Typed> supertraits, Position position) {
		super(position);
		this.name = name;
		this.superclass = superclass;
		this.supertraits = supertraits;
	}

}