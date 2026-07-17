package com.lang.symbol;

public class ParamSymbol extends Symbol {
	private final  String name;
	private final Symbol type;

	public ParamSymbol(String name, Symbol type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public Symbol getType() {
		return type;
	}
}