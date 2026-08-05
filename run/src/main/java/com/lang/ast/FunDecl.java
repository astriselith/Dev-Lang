package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class FunDecl extends MemberDecl {
	public List<TypeParamDecl> typeParameters;
	public List<ParamDecl> parameters;
	public Typed returnType;
	public BlockStmt body;

	public FunDecl() {
	}

	public FunDecl(Identifier name, List<TypeParamDecl> typeParameters, List<ParamDecl> parameters,
			Typed returnType, BlockStmt body, Position position) {
		super(name, position);
		this.typeParameters = typeParameters;
		this.parameters = parameters;
		this.returnType = returnType;
		this.body = body;
	}
}