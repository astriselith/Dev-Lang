package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class ClassDeclStmt extends Stmt {
	public String name;

	public List<Typed> superclasses;
	public List<TypeParamDeclStmt> typeParameters;

	public List<FunDeclStmt> funs;
	public List<VarDeclStmt> vars;

	public ClassDeclStmt() {
	}

	public ClassDeclStmt(String name, List<Typed> superclasses, List<TypeParamDeclStmt> typeParameters,
			List<FunDeclStmt> funs, List<VarDeclStmt> vars, Position position) {
		super(position);
		this.name = name;
		this.superclasses = superclasses;
		this.typeParameters = typeParameters;
		this.funs = funs;
		this.vars = vars;
	}
}