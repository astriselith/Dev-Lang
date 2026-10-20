package com.lang.symbol;

import com.lang.ast.Expr;

public class VarSymbol extends MemberSymbol {
	private Expr initializer;

	public VarSymbol(String name) {
		super(name);
	}

	public void setInitializer(Expr initializer) {
		this.initializer = initializer;
	}

	public Expr getInitializer() {
		return initializer;
	}
}