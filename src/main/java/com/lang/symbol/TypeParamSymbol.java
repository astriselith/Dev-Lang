package com.lang.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypeParamSymbol extends Symbol {

	private Map<String, SuperSymbol> supers;

	public TypeParamSymbol(String name) {
		super(name);
		this.supers = new LinkedHashMap<>();
	}

	// Supers

	public void addSuper(SuperSymbol symbol) {
		this.supers.put(symbol.getName(), symbol);
	}

	public void setSupers(Map<String, SuperSymbol> supers) {
		this.supers.clear();
		if (supers != null)
			this.supers.putAll(supers);
	}

	public boolean hasSuper(String className) {
		return this.supers.containsKey(className);
	}

	public SuperSymbol getSuper(String className) {
		return this.supers.get(className);
	}

	public Map<String, SuperSymbol> getSupers() {
		return Collections.unmodifiableMap(supers);
	}

	public boolean hasSupers() {
		return !supers.isEmpty();
	}

	// Supers (util)

	public List<ClassSymbol> getBaseSupers() {
		List<ClassSymbol> result = new ArrayList<>();
		for (SuperSymbol sup : supers.values()) {
			Symbol base = sup.getType();
			if (base == null)
				continue;
			if (base.isClass()) {
				result.add(base.asClass());
			} else if (base.isParameterizedClass()) {
				result.add(base.asParameterizedClass().getBase());
			}
		}
		return Collections.unmodifiableList(result);
	}

	public Symbol getMember(String name) {
		for (ClassSymbol supers : getBaseSupers()) {
			Symbol result = supers.getMember(name);
			if (result != null)
				return result;
		}

		return null;
	}
}