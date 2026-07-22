package com.lang.symbol;

import java.util.Collections;
import java.util.List;

public class TypeParamSymbol extends Symbol {
	private final String name;
	private final List<Symbol> superclasses;

	public TypeParamSymbol(String name, List<Symbol> superclasses) {
		this.name = name;
		this.superclasses = superclasses != null
				? Collections.unmodifiableList(superclasses)
				: Collections.emptyList();
	}

	public String getName() {
		return name;
	}

	public List<Symbol> getSuperclasses() {
		return superclasses;
	}

	public boolean hasSuperclasses() {
		return !superclasses.isEmpty();
	}

	private List<ClassSymbol> getBaseSuperclasses() {
		if (superclasses.isEmpty())
			return Collections.emptyList();

		List<ClassSymbol> result = new java.util.ArrayList<>();
		for (Symbol trait : superclasses) {
			if (trait.isClass()) {
				result.add(trait.asClass());
			} else if (trait.isParameterized()) {
				result.add(trait.asParameterized().getBase());
			}
		}
		return Collections.unmodifiableList(result);
	}

	public FunSymbol getFun(String name) {
		for (ClassSymbol trait : getBaseSuperclasses()) {
			FunSymbol result = trait.getFun(name);
			if (result != null)
				return result;
		}

		return null;
	}

	public VarSymbol getVar(String name) {
		for (ClassSymbol trait : getBaseSuperclasses()) {
			VarSymbol result = trait.getVar(name);
			if (result != null)
				return result;
		}

		return null;
	}
}