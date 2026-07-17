package com.lang.symbol;


public class VarSymbol extends Symbol {
	private final String name;
	private final ClassSymbol declaringClass;
	private final Symbol type;

	public VarSymbol(String name, ClassSymbol declaringClass, Symbol type) {
		this.name = name;
		this.declaringClass = declaringClass;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public ClassSymbol getDeclaringClass() {
		return declaringClass;
	}

	public Symbol getType() {
		return type;
	}
}