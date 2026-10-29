package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class FunDecl extends MemberDecl {
	public List<TypeParamDecl> typeParameters;
	public List<ParamDecl> parameters;
	public BlockStmt body;

	public FunDecl() {
	}

	public FunDecl(Identifier name, Typed type, List<TypeParamDecl> typeParameters, List<ParamDecl> parameters, BlockStmt body, Position position) {
		super(name, type, position);
		this.typeParameters = typeParameters;
		this.parameters = parameters;
		this.body = body;
	}

	@Override
	public <T> T accept(DeclVisitor<T> visitor) {
		return visitor.visitFunDecl(this);
	}
}