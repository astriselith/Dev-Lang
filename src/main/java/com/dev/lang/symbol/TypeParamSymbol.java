package com.dev.lang.symbol;

import java.util.Collections;
import java.util.List;

public class TypeParamSymbol extends Symbol {
	private final String name;
	private final Symbol superclass;
	private final List<Symbol> supertraits;

	public TypeParamSymbol(String name, Symbol superclass, List<Symbol> supertraits) {
		this.name = name;
		this.superclass = superclass;
		this.supertraits = supertraits != null
						   ? Collections.unmodifiableList(supertraits)
						   : Collections.emptyList();
	}

	public String getName() {
		return name;
	}

	public Symbol getSuperclass() {
		return superclass;
	}

	public List<Symbol> getSupertraits() {
		return supertraits;
	}

	public boolean hasSuperclass() {
		return superclass != null;
	}

	public boolean hasSupertraits() {
		return !supertraits.isEmpty();
	}

	private ClassSymbol getBaseSuperclass() {
		if (superclass == null) return null;

		if (superclass.isClass()) {
			return superclass.asClass();
		}

		if (superclass.isParameterized()) {
			return superclass.asParameterized().getBase();
		}

		return null;
	}

	private List<ClassSymbol> getBaseSupertraits() {
		if (supertraits.isEmpty()) return Collections.emptyList();

		List<ClassSymbol> result = new java.util.ArrayList<>();
		for (Symbol trait : supertraits) {
			if (trait.isClass()) {
				result.add(trait.asClass());
			} else if (trait.isParameterized()) {
				result.add(trait.asParameterized().getBase());
			}
		}
		return Collections.unmodifiableList(result);
	}

	public Symbol getMember(String name) {
		ClassSymbol baseSuperclass = getBaseSuperclass();
		if (baseSuperclass != null) {
			Symbol result = baseSuperclass.getMember(name);
			if (result != null) return result;
		}

		for (ClassSymbol trait : getBaseSupertraits()) {
			Symbol result = trait.getMember(name);
			if (result != null) return result;
		}

		return null;
	}

	public FunSymbol getFun(String name) {
		ClassSymbol baseSuperclass = getBaseSuperclass();
		if (baseSuperclass != null) {
			FunSymbol result = baseSuperclass.getFun(name);
			if (result != null) return result;
		}

		for (ClassSymbol trait : getBaseSupertraits()) {
			FunSymbol result = trait.getFun(name);
			if (result != null) return result;
		}

		return null;
	}

	public VarSymbol getVar(String name) {
		ClassSymbol baseSuperclass = getBaseSuperclass();
		if (baseSuperclass != null) {
			VarSymbol result = baseSuperclass.getVar(name);
			if (result != null) return result;
		}

		for (ClassSymbol trait : getBaseSupertraits()) {
			VarSymbol result = trait.getVar(name);
			if (result != null) return result;
		}

		return null;
	}
}