package com.dev.lang.symbol;

import com.dev.lang.ast.Modifier;

public class VarSymbol extends Symbol {
	private final Modifier modifier;
	private final String name;
	private final ClassOrTraitSymbol declaringClassOrTrait;
	private final Symbol type;
	private boolean initialized;

	public VarSymbol(Modifier modifier, String name, ClassOrTraitSymbol declaringClassOrTrait, Symbol type, boolean initialized) {
		this.modifier = modifier;
		this.name = name;
		this.declaringClassOrTrait = declaringClassOrTrait;
		this.type = type;
		this.initialized = initialized;
	}

	public Modifier getModifier() {
		return modifier;
	}

	public String getName() {
		return name;
	}

	public ClassOrTraitSymbol getDeclaringClassOrTrait() {
		return declaringClassOrTrait;
	}

	public Symbol getType() {
		return type;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}
}