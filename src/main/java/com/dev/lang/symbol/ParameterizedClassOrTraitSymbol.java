package com.dev.lang.symbol;

import java.util.*;

public class ParameterizedClassOrTraitSymbol extends Symbol {
	private final ClassOrTraitSymbol base;
	private final Map<String, Symbol> typeArguments;

	public ParameterizedClassOrTraitSymbol(ClassOrTraitSymbol base, Map<String, Symbol> typeArguments) {
		this.base = base;
		this.typeArguments = typeArguments != null
							 ? Collections.unmodifiableMap(typeArguments)
							 : Collections.emptyMap();
	}

	public ClassOrTraitSymbol getBase() {
		return base;
	}

	public Map<String, Symbol> getTypeArguments() {
		return typeArguments;
	}
}