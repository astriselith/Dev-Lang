package com.dev.lang.symbol;


public class VarSymbol extends Symbol {
	private final String name;
	private final ClassSymbol declaringClassOrTrait;
	private final Symbol type;

	public VarSymbol(String name, ClassSymbol declaringClassOrTrait, Symbol type) {
		this.name = name;
		this.declaringClassOrTrait = declaringClassOrTrait;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public ClassSymbol getDeclaringClassOrTrait() {
		return declaringClassOrTrait;
	}

	public Symbol getType() {
		return type;
	}
}