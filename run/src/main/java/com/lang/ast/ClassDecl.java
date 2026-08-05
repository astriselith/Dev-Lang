package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class ClassDecl extends MemberDecl {

	public List<SuperDecl> supers;
	public List<TypeParamDecl> typeParameters;

	public List<MemberDecl> members;

	public ClassDecl() {
	}

	public ClassDecl(Identifier name, List<SuperDecl> supers, List<TypeParamDecl> typeParameters,
			List<MemberDecl> members, Position position) {
		super(name, position);

		this.supers = supers;
		this.typeParameters = typeParameters;
		this.members = members;
	}

	@Override
	public String toString() {
		return name.toString();
	}

}