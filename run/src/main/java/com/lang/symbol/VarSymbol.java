package com.lang.symbol;

import com.lang.ast.Typed;

public class VarSymbol extends Symbol {
	private final String name;
	private final Typed typeType;
	private Symbol type;

	public VarSymbol(String name, Typed typeType) {
		this.name = name;
		this.typeType = typeType;
	}

	public String getName() {
		return name;
	}

	public Typed getTypeType() {
		return typeType;
	}

	public void setType(Symbol type) {
		this.type = type;
	}

	public Symbol getType() {
		return type;
	}
}