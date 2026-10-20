package com.lang.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClassSymbol extends Symbol {

	private final Map<String, TypeParamSymbol> typeParameters;
	private final Map<String, ParamSymbol> parameters;
	private final Map<String, SuperSymbol> supers;
	private final Map<String, MemberSymbol> declaredMembers;

	private boolean pure = false;

	public ClassSymbol(String name) {
		super(name);

		this.typeParameters = new LinkedHashMap<>();
		this.parameters = new LinkedHashMap<>();
		this.supers = new LinkedHashMap<>();
		this.declaredMembers = new LinkedHashMap<>();
	}

	// TypeParameters

	public void addTypeParameter(TypeParamSymbol tpSymbol) {
		this.typeParameters.put(tpSymbol.getName(), tpSymbol);
	}

	public void setTypeParameters(Map<String, TypeParamSymbol> typeParameters) {
		this.typeParameters.clear();
		if (typeParameters != null)
			this.typeParameters.putAll(typeParameters);
	}

	public TypeParamSymbol getTypeParameter(String tpName) {
		return this.typeParameters.get(tpName);
	}

	public Map<String, TypeParamSymbol> getTypeParameters() {
		return Collections.unmodifiableMap(typeParameters);
	}

	public boolean hasTypeParameter(String tpName) {
		return this.typeParameters.containsKey(tpName);
	}

	public boolean hasTypeParameters() {
		return !typeParameters.isEmpty();
	}

	// Parameters

	public void addParameter(ParamSymbol pSymbol) {
		this.parameters.put(pSymbol.getName(), pSymbol);
	}

	public void setParameters(Map<String, ParamSymbol> parameters) {
		this.parameters.clear();
		if (parameters != null)
			this.parameters.putAll(parameters);
	}

	public ParamSymbol getParameter(String pName) {
		return this.parameters.get(pName);
	}

	public Map<String, ParamSymbol> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}

	public boolean hasParameter(String pName) {
		return this.parameters.containsKey(pName);
	}

	public boolean hasParameters() {
		return !parameters.isEmpty();
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

	// Members

	public void addDeclaredMember(MemberSymbol member) {
		if (member != null)
			declaredMembers.put(member.getName(), member);
	}

	public MemberSymbol getDeclaredMember(String name) {
		return declaredMembers.get(name);
	}

	public Map<String, MemberSymbol> getDeclaredMembers() {
		return Collections.unmodifiableMap(declaredMembers);
	}

	public boolean hasDeclaredMember(String name) {
		return this.declaredMembers.containsKey(name);
	}

	public boolean hasDeclaredMembers() {
		return !this.declaredMembers.isEmpty();
	}

	// Members (util)

	public MemberSymbol getMember(String name) {
		MemberSymbol member = declaredMembers.get(name);
		if (member != null)
			return member;

		List<ClassSymbol> baseSupers = getBaseSupers();
		for (ClassSymbol clazz : baseSupers) {
			member = clazz.getMember(name);
			if (member != null)
				return member;
		}
		return null;
	}

	public boolean hasMember(String name) {
		return getMember(name) != null;
	}

	// Pure

	public void setPure(boolean pure) {
		this.pure = pure;
	}

	public boolean isPure() {
		return this.pure;
	}
}