package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class FunDeclStmt extends Stmt {
	public Identifier name;
	public List<TypeParamDeclStmt> typeParameters;
	public List<ParamDeclStmt> parameters;
	public Typed returnType;
	public BlockStmt body;

	public FunDeclStmt() {
	}

	public FunDeclStmt(Identifier name, List<TypeParamDeclStmt> typeParameters, List<ParamDeclStmt> parameters,
			Typed returnType, BlockStmt body, Position position) {
		super(position);
		this.name = name;
		this.typeParameters = typeParameters;
		this.parameters = parameters;
		this.returnType = returnType;
		this.body = body;
	}
}