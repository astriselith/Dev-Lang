package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class TypeParamDecl extends Decl {
	public Identifier name;
	public List<BoundDecl> bounds;

	public TypeParamDecl() {
	}

	public TypeParamDecl(Identifier name, List<BoundDecl> bounds,
			Position position) {
		super(position);
		this.name = name;
		this.bounds = bounds;
	}
}