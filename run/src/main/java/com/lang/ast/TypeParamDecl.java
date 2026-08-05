package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class TypeParamDecl extends Decl {
	public Identifier name;
	public List<SuperDecl> supers;

	public TypeParamDecl() {
	}

	public TypeParamDecl(Identifier name, List<SuperDecl> supers,
			Position position) {
		super(position);
		this.name = name;
		this.supers = supers;
	}
}