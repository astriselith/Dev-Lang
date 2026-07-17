package com.lang.symbol;

import java.util.*;
import com.lang.ast.Typed;

public class ClassSymbol extends Symbol {
	private final String name;

	private final List<String> typeParameterTypes;
	private final List<Typed> supertraitTypes;
	private final Typed superclassType;

	private List<TypeParamSymbol> typeParameters;
	private List<Symbol> supertraits;
	private Symbol superclass;
	private Map<String, Symbol> members;
	private Map<String, FunSymbol> funs;
	private Map<String, VarSymbol> vars;

	public ClassSymbol(String name,
							  List<String> typeParameterTypes,
							  List<Typed> supertraitTypes,
							  Typed superclassType) {
		this.name = name;
		
		this.typeParameterTypes = typeParameterTypes != null ? new ArrayList<>(typeParameterTypes) : new ArrayList<>();
		this.supertraitTypes = supertraitTypes != null ? new ArrayList<>(supertraitTypes) : new ArrayList<>();
		this.superclassType = superclassType;

		this.typeParameters = new ArrayList<>();
		this.superclass = null;
		this.supertraits = new ArrayList<>();
		this.members = new HashMap<>();
		this.funs = new HashMap<>();
		this.vars = new HashMap<>();
	}

	public String getName() {
		return name;
	}

	public List<String> getTypeParameterTypes() {
		return Collections.unmodifiableList(typeParameterTypes);
	}

	public List<Typed> getSupertraitTypes() {
		return Collections.unmodifiableList(supertraitTypes);
	}

	public Typed getSuperclassType() {
		return superclassType;
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
	public Symbol getSuperclass() {
		return superclass;
	}

	public ClassSymbol getBaseSuperclass() {
		if (superclass == null) return null;

		if (superclass.isClass()) {
			return superclass.asClass();
		}

		if (superclass.isParameterized()) {
			return superclass.asParameterized().getBase();
		}

		return null;
	}

	public void setSuperclass(Symbol superclass) {
		this.superclass = superclass;
	}

	public boolean hasSuperclass() {
		return superclass != null;
	}


	public List<Symbol> getSupertraits() {
		return Collections.unmodifiableList(supertraits);
	}

	public List<ClassSymbol> getBaseSupertraits() {
		List<ClassSymbol> result = new ArrayList<>();
		for (Symbol trait : supertraits) {
			if (trait.isClass()) {
				result.add(trait.asClass());
			} else if (trait.isParameterized()) {
				result.add(trait.asParameterized().getBase());
			}
		}
		return Collections.unmodifiableList(result);
	}

	public void setSupertraits(List<Symbol> traits) {
		this.supertraits.clear();
		if (traits != null) {
			for (Symbol trait : traits) {
				if (trait != null) {
					if (this.supertraits.contains(trait)) continue;
					this.supertraits.add(trait);
				}
			}
		}
	}

	public boolean hasSupertraits() {
		return !supertraits.isEmpty();
	}

	
	public void addMember(Symbol member) {
		if (member == null) return;

		if (member.isFun()) {
			addFun(member.asFun());
		} else if (member.isVar()) {
			addVar(member.asVar());
		}
	}

	public void addFun(FunSymbol fun) {
		if (fun != null) {
			String name = fun.getName();
			members.put(name, fun);
			funs.put(name, fun);
		}
	}

	public void addVar(VarSymbol var) {
		if (var != null) {
			String name = var.getName();
			members.put(name, var);
			vars.put(name, var);
		}
	}

	public Symbol getMember(String name) {
		Symbol member = members.get(name);
		if (member != null) return member;
		ClassSymbol baseSuperclass = getBaseSuperclass();
		if (baseSuperclass != null) {
			member = baseSuperclass.getMember(name);
			if (member != null) return member;
		}
		List<ClassSymbol> baseSupertraits = getBaseSupertraits();
		for (ClassSymbol trait : baseSupertraits) {
			member = trait.getMember(name);
			if (member != null) return member;
		}
		return null;
	}

	public FunSymbol getFun(String name) {
		FunSymbol fun = funs.get(name);
		if (fun != null) return fun;

		ClassSymbol baseSuperclass = getBaseSuperclass();
		if (baseSuperclass != null) {
			fun = baseSuperclass.getFun(name);
			if (fun != null) return fun;
		}
		
		List<ClassSymbol> baseSupertraits = getBaseSupertraits();
		for (ClassSymbol trait : baseSupertraits) {
			fun = trait.getFun(name);
			if (fun != null) return fun;
		}
		return null;
	}

	public VarSymbol getVar(String name) {
		VarSymbol var = vars.get(name);
		if (var != null) return var;

		ClassSymbol baseSuperclass = getBaseSuperclass();
		if (baseSuperclass != null) {
			var = baseSuperclass.getVar(name);
			if (var != null) return var;
		}

		List<ClassSymbol> baseSupertraits = getBaseSupertraits();
		for (ClassSymbol trait : baseSupertraits) {
			var = trait.getVar(name);
			if (var != null) return var;
		}
		return null;
	}

	public boolean hasMember(String name) {
		return getMember(name) != null;
	}

	public boolean hasFun(String name) {
		return getFun(name) != null;
	}

	public boolean hasVar(String name) {
		return getVar(name) != null;
	}

	public Map<String, Symbol> getDeclaredMembers() {
		return Collections.unmodifiableMap(members);
	}

	public Map<String, FunSymbol> getDeclaredFuns() {
		return Collections.unmodifiableMap(funs);
	}

	public Map<String, VarSymbol> getDeclaredVars() {
		return Collections.unmodifiableMap(vars);
	}

	public Symbol getDeclaredMember(String name) {
		return members.get(name);
	}

	public FunSymbol getDeclaredFun(String name) {
		return funs.get(name);
	}

	public VarSymbol getDeclaredVar(String name) {
		return vars.get(name);
	}
}