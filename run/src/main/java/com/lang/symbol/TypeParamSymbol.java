package com.lang.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypeParamSymbol extends Symbol {

	private Map<String, Symbol> superclasses;

	public TypeParamSymbol(String name) {
		super(name);
		this.superclasses = new LinkedHashMap<>();
	}

	// Superclasses

	public void addSuperclass(Symbol symbol) {
		String className = null;
		if (symbol.isClass()) {
			className = symbol.asClass().getName();
		}
		if (symbol.isParameterizedClass()) {
			className = symbol.asParameterizedClass().getBase().getName();
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
			} else if (clazz.isParameterizedClass()) {
				result.add(clazz.asParameterizedClass().getBase());
			}
		}
		return Collections.unmodifiableList(result);
	}

	public Symbol getMember(String name) {
		for (ClassSymbol superclass : getBaseSuperclasses()) {
			Symbol result = superclass.getMember(name);
			if (result != null)
				return result;
		}

		return null;
	}

}