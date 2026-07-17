package com.lang.symbol;

public class ParamSymbol extends Symbol {
	private final  String name;
	private final Symbol type;
	private final FunSymbol enclosingFun;

	public ParamSymbol(String name, Symbol type, FunSymbol enclosingFun) {
		this.name = name;
		this.type = type;
		this.enclosingFun = enclosingFun;
	}

	public String getName() {
		return name;
	}

	public Symbol getType() {
		return type;
	}

	public FunSymbol getEnclosingFun() {
		return enclosingFun;
	}
}