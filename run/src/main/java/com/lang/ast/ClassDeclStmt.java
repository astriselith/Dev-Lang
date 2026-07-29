package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class ClassDeclStmt extends Stmt {
	public Identifier name;

	public List<Typed> superclasses;
	public List<TypeParamDeclStmt> typeParameters;

	public List<MemberDeclStmt> members;

	public ClassDeclStmt() {
	}

	public ClassDeclStmt(Identifier name, List<Typed> superclasses, List<TypeParamDeclStmt> typeParameters,
			List<MemberDeclStmt> members, Position position) {
		super(position);
		this.name = name;
		this.superclasses = superclasses;
		this.typeParameters = typeParameters;
		this.members = members;
	}
}