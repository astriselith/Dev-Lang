package com.lang.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.lang.ast.Typed;

public class TypeParamSymbol extends Symbol {
	private final String name;
	private final List<Typed> superclassTypes;
	private Map<String, Symbol> superclasses;

	public TypeParamSymbol(String name, List<Typed> superclassTypes) {
		this.name = name;
		this.superclassTypes = superclassTypes;

		this.superclasses = new LinkedHashMap<>();
	}

	public String getName() {
		return name;
	}

	public List<Typed> getSuperclassTypes() {
		return Collections.unmodifiableList(superclassTypes);
	}

	// Superclasses

	public void addSuperclass(Symbol symbol) {
		String className = null;
		if (symbol.isClass()) {
			className = symbol.asClass().getName();
		}
		if (symbol.isParameterized()) {
			className = symbol.asParameterized().getBase().getName();
		}
		if (className != null)
			this.superclasses.put(className, symbol);
	}

	public void setSuperclasses(Map<String, Symbol> superclasses) {
		this.superclasses.clear();
		if (superclasses != null)
			this.superclasses.putAll(superclasses);
	}

	public boolean hasSuperclass(String className) {
		return this.superclasses.containsKey(className);
	}

	public Symbol getSuperclass(String className) {
		return this.superclasses.get(className);
	}

	public Map<String, Symbol> getSuperclasses() {
		return Collections.unmodifiableMap(superclasses);
	}

	public boolean hasSuperclasses() {
		return !superclasses.isEmpty();
	}

	// Superclasses (util)

	public List<ClassSymbol> getBaseSuperclasses() {
		List<ClassSymbol> result = new ArrayList<>();
		for (Symbol clazz : superclasses.values()) {
			if (clazz.isClass()) {
				result.add(clazz.asClass());
			} else if (clazz.isParameterized()) {
				result.add(clazz.asParameterized().getBase());
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