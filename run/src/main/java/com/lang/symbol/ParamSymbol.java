package com.lang.symbol;

public class ParamSymbol extends Symbol {
	private Symbol type;

	public ParamSymbol(String name) {
		super(name);
	}

	public void setType(Symbol type) {
		this.type = type;
	}

	public Symbol getType() {
		return type;
	}
}