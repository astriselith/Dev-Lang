package com.lang.symbol;

import com.lang.ast.Expr;

public class VarSymbol extends MemberSymbol {
	private Expr init;

	public VarSymbol(String name) {
		super(name);
	}

	public void setInit(Expr init) {
		this.init = init;
	}

	public Expr getInit() {
		return init;
	}

	public boolean hasInit() {
		return init != null;
	}
}