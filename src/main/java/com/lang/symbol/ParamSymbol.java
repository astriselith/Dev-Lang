package com.lang.symbol;

import com.lang.ast.Expr;

public class ParamSymbol extends Symbol {
	private Symbol type;
	private Expr init;

	public ParamSymbol(String name) {
		super(name);
	}

	public void setType(Symbol type) {
		this.type = type;
	}

	public Symbol getType() {
		return type;
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