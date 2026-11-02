package com.lang.symbol;

import java.util.*;

public class ParameterizedClassSymbol extends Symbol {
	private final ClassSymbol base;
	private final Map<String, Symbol> typeArguments;

	public ParameterizedClassSymbol(ClassSymbol base, Map<String, Symbol> typeArguments) {
		super(base.getName());
		this.base = base;
		this.typeArguments = typeArguments != null
				? Collections.unmodifiableMap(typeArguments)
				: Collections.emptyMap();
	}

	public ClassSymbol getBase() {
		return base;
	}

	public Map<String, Symbol> getTypeArguments() {
		return typeArguments;
	}
}