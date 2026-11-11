package com.lang.ast;

import java.util.List;
import com.lang.util.Position;

public class FunDecl extends MemberDecl {
	public Typed type;
	public List<TypeParamDecl> typeParameters;
	public List<ParamDecl> parameters;
	public BlockStmt body;

	public FunDecl() {
	}

	public FunDecl(List<AnnoDecl> annotations, List<Modifier> modifiers,
			Identifier name, Typed type,
			List<TypeParamDecl> typeParameters,
			List<ParamDecl> parameters, BlockStmt body, Position position) {
		super(annotations, modifiers, name, position);
		this.type = type;
		this.typeParameters = typeParameters;
		this.parameters = parameters;
		this.body = body;
	}

	@Override
	public <T> T accept(DeclVisitor<T> visitor) {
		return visitor.visitFunDecl(this);
	}
}