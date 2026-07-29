package com.lang.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClassSymbol extends Symbol {

	private final Map<String, TypeParamSymbol> typeParameters;
	private final Map<String, Symbol> superclasses;
	private final Map<String, MemberSymbol> declaredMembers;

	private boolean pure = false;

	public ClassSymbol(String name) {
		super(name);

		this.typeParameters = new LinkedHashMap<>();
		this.superclasses = new LinkedHashMap<>();
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

	public boolean hasDeclaredMember(String funName) {
		return this.declaredMembers.containsKey(funName);
	}

	public boolean hasDeclaredMembers() {
		return this.declaredMembers.isEmpty();
	}

	// Functions (util)

	public MemberSymbol getMember(String name) {
		MemberSymbol member = declaredMembers.get(name);
		if (member != null)
			return member;

		List<ClassSymbol> baseSuperclasss = getBaseSuperclasses();
		for (ClassSymbol clazz : baseSuperclasss) {
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