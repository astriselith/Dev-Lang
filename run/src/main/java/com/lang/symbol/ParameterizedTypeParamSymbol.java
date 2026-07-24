package com.lang.symbol;

import java.util.*;

public class ParameterizedTypeParamSymbol extends Symbol {
	private final TypeParamSymbol base;
	private final Map<String, Symbol> typeArguments;

	public ParameterizedTypeParamSymbol(TypeParamSymbol base, Map<String, Symbol> typeArguments) {
		this.base = base;
		this.typeArguments = typeArguments != null
				? Collections.unmodifiableMap(typeArguments)
				: Collections.emptyMap();
	}

	public TypeParamSymbol getBase() {
		return base;
	}

	public Map<String, Symbol> getTypeArguments() {
		return typeArguments;
	}
}