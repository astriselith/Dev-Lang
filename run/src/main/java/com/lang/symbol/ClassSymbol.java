package com.lang.symbol;

import java.util.*;
import com.lang.ast.Typed;

public class ClassSymbol extends Symbol {
	private final String name;

	private final List<String> typeParameterTypes;
	private final List<Typed> superclassTypes;

	private Map<String, TypeParamSymbol> typeParameters;
	private Map<String, Symbol> superclasses;
	private Map<String, FunSymbol> declaredFuns;
	private Map<String, VarSymbol> declaredVars;

	private boolean pure = false;

	public ClassSymbol(String name,
			List<String> typeParameterTypes,
			List<Typed> superclassTypes) {
		this.name = name;

		this.typeParameterTypes = typeParameterTypes != null ? new ArrayList<>(typeParameterTypes) : new ArrayList<>();
		this.superclassTypes = superclassTypes != null ? new ArrayList<>(superclassTypes) : new ArrayList<>();

		this.typeParameters = new LinkedHashMap<>();
		this.superclasses = new LinkedHashMap<>();
		this.declaredFuns = new LinkedHashMap<>();
		this.declaredVars = new LinkedHashMap<>();
	}

	public String getName() {
		return name;
	}

	public List<String> getTypeParameterTypes() {
		return Collections.unmodifiableList(typeParameterTypes);
	}

	public List<Typed> getSuperclassTypes() {
		return Collections.unmodifiableList(superclassTypes);
	}

	// TypeParameters

	public void addTypeParameter(String tpName, TypeParamSymbol tpSymbol) {
		this.typeParameters.put(tpName, tpSymbol);
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

	// Variables

	public void addDeclaredVar(VarSymbol var) {
		if (var != null) {
			String name = var.getName();
			declaredVars.put(name, var);
		}
	}

	public void setDeclaredVars(List<VarSymbol> declaredVars) {
		this.declaredVars.clear();
		if (declaredVars != null)
			declaredVars.forEach(varSymbol -> this.declaredVars.put(varSymbol.getName(), varSymbol));
	}

	public VarSymbol getDeclaredVar(String name) {
		return declaredVars.get(name);
	}

	public Map<String, VarSymbol> getDeclaredVars() {
		return Collections.unmodifiableMap(declaredVars);
	}

	public boolean hasDeclaredVar(String varName) {
		return this.declaredVars.containsKey(varName);
	}

	public boolean hasDeclaredVars() {
		return this.declaredVars.isEmpty();
	}

	// Variables (util)

	public VarSymbol getVar(String name) {
		VarSymbol var = declaredVars.get(name);
		if (var != null)
			return var;

		List<ClassSymbol> baseSuperclasss = getBaseSuperclasses();
		for (ClassSymbol clazz : baseSuperclasss) {
			var = clazz.getVar(name);
			if (var != null)
				return var;
		}
		return null;
	}

	public boolean hasVar(String name) {
		return getVar(name) != null;
	}

	// Functions

	public void addDeclaredFun(FunSymbol fun) {
		if (fun != null) {
			String name = fun.getName();
			declaredFuns.put(name, fun);
		}
	}

	public void setDeclaredFuns(List<FunSymbol> declaredFuns) {
		this.declaredFuns.clear();
		if (declaredFuns != null)
			declaredFuns.forEach(funSymbol -> this.declaredFuns.put(funSymbol.getName(), funSymbol));
	}

	public FunSymbol getDeclaredFun(String name) {
		return declaredFuns.get(name);
	}

	public Map<String, FunSymbol> getDeclaredFuns() {
		return Collections.unmodifiableMap(declaredFuns);
	}

	public boolean hasDeclaredFun(String funName) {
		return this.declaredFuns.containsKey(funName);
	}

	public boolean hasDeclaredFuns() {
		return this.declaredFuns.isEmpty();
	}

	// Functions (util)

	public FunSymbol getFun(String name) {
		FunSymbol fun = declaredFuns.get(name);
		if (fun != null)
			return fun;

		List<ClassSymbol> baseSuperclasss = getBaseSuperclasses();
		for (ClassSymbol clazz : baseSuperclasss) {
			fun = clazz.getFun(name);
			if (fun != null)
				return fun;
		}
		return null;
	}

	public boolean hasFun(String name) {
		return getFun(name) != null;
	}

	// Pure

	public void setPure(boolean pure) {
		this.pure = pure;
	}

	public boolean isPure() {
		return this.pure;
	}
}