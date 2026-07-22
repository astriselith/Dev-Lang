package com.lang.symbol;

import java.util.*;
import com.lang.ast.Typed;

public class ClassSymbol extends Symbol {
	private final String name;

	private final List<String> typeParameterTypes;
	private final List<Typed> superclassTypes;

	private List<TypeParamSymbol> typeParameters;
	private List<Symbol> superclasses;
	private Map<String, FunSymbol> funs;
	private Map<String, VarSymbol> vars;

	private boolean pure = false;

	public ClassSymbol(String name,
			List<String> typeParameterTypes,
			List<Typed> superclassTypes) {
		this.name = name;

		this.typeParameterTypes = typeParameterTypes != null ? new ArrayList<>(typeParameterTypes) : new ArrayList<>();
		this.superclassTypes = superclassTypes != null ? new ArrayList<>(superclassTypes) : new ArrayList<>();

		this.typeParameters = new ArrayList<>();
		this.superclasses = new ArrayList<>();
		this.funs = new HashMap<>();
		this.vars = new HashMap<>();
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

	public List<TypeParamSymbol> getTypeParameters() {
		return Collections.unmodifiableList(typeParameters);
	}

	public void setTypeParameters(List<TypeParamSymbol> typeParameters) {
		this.typeParameters.clear();
		if (typeParameters != null) {
			this.typeParameters.addAll(typeParameters);
		}
	}

	public boolean hasTypeParameters() {
		return !typeParameters.isEmpty();
	}

	public List<Symbol> getSuperclasses() {
		return Collections.unmodifiableList(superclasses);
	}

	public List<ClassSymbol> getBaseSuperclasses() {
		List<ClassSymbol> result = new ArrayList<>();
		for (Symbol clazz : superclasses) {
			if (clazz.isClass()) {
				result.add(clazz.asClass());
			} else if (clazz.isParameterized()) {
				result.add(clazz.asParameterized().getBase());
			}
		}
		return Collections.unmodifiableList(result);
	}

	public void addSuperclass(Symbol clazz) {
		if (clazz == null)
			return;
		if (superclasses.contains(clazz))
			return;
		superclasses.add(clazz);
	}

	public void setSuperclasses(List<Symbol> classes) {
		this.superclasses.clear();
		if (classes != null) {
			for (Symbol clazz : classes) {
				if (clazz != null) {
					if (this.superclasses.contains(clazz))
						continue;
					this.superclasses.add(clazz);
				}
			}
		}
	}

	public boolean hasSuperclasses() {
		return !superclasses.isEmpty();
	}

	public void addFun(FunSymbol fun) {
		if (fun != null) {
			String name = fun.getName();
			funs.put(name, fun);
		}
	}

	public void addVar(VarSymbol var) {
		if (var != null) {
			String name = var.getName();
			vars.put(name, var);
		}
	}

	public FunSymbol getFun(String name) {
		FunSymbol fun = funs.get(name);
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

	public VarSymbol getVar(String name) {
		VarSymbol var = vars.get(name);
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

	public boolean hasFun(String name) {
		return getFun(name) != null;
	}

	public boolean hasVar(String name) {
		return getVar(name) != null;
	}

	public Map<String, FunSymbol> getDeclaredFuns() {
		return Collections.unmodifiableMap(funs);
	}

	public Map<String, VarSymbol> getDeclaredVars() {
		return Collections.unmodifiableMap(vars);
	}

	public FunSymbol getDeclaredFun(String name) {
		return funs.get(name);
	}

	public VarSymbol getDeclaredVar(String name) {
		return vars.get(name);
	}

	public void setPure(boolean pure) {
		this.pure = pure;
	}

	public boolean isPure() {
		return this.pure;
	}
}