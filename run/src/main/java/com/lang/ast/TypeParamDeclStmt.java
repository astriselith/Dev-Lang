package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class TypeParamDeclStmt extends Stmt {
	public Identifier name;
	public List<TypeParamDeclStmt> typeParameters;
	public List<Typed> superclasses;

	public TypeParamDeclStmt() {
	}

	public TypeParamDeclStmt(Identifier name, List<TypeParamDeclStmt> typeParameters, List<Typed> superclasses,
			Position position) {
		super(position);
		this.name = name;
		this.typeParameters = typeParameters;
		this.superclasses = superclasses;
	}
}