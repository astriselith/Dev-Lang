package com.dev.lang.symbol;

public class ParamSymbol extends Symbol {
	private final  String name;
	private final Symbol type;
	private final FunSymbol enclosingFunction;

	public ParamSymbol(String name, Symbol type, FunSymbol enclosingFunction) {
		this.name = name;
		this.type = type;
		this.enclosingFunction = enclosingFunction;
	}

	public String getName() {
		return name;
	}

	public Symbol getType() {
		return type;
	}

	public FunSymbol getEnclosingFunction() {
		return enclosingFunction;
	}
}