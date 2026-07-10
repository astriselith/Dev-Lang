package com.dev.lang.ast;

import com.dev.lang.util.Position;
import java.util.List;

public class TypeParamDeclStmt extends Stmt {

	public final String name;
	public final Typed  superclass;
	public final List<Typed> supertraits;

	public TypeParamDeclStmt(String name, Typed superclass, List<Typed> supertraits, Position position) {
		super(position);
		this.name = name;
		this.superclass = superclass;
		this.supertraits = supertraits;
	}
}