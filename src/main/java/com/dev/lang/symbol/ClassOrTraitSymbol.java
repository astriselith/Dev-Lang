package com.dev.lang.symbol;

import java.util.*;
import com.dev.lang.ast.Modifier;
import com.dev.lang.ast.Typed;
import com.dev.lang.ast.Kind;

public class ClassOrTraitSymbol extends Symbol {
	private final Modifier modifier;
	private final String name;
	private final Kind kind;

	private final List<String> typeParameterTypes;
	private final List<Typed> supertraitTypes;
	private final Typed superclassType;

	private List<TypeParamSymbol> typeParameters;
	private List<Symbol> supertraits;
	private Symbol superclass;
	private Map<String, Symbol> members;
	private Map<String, FunSymbol> funs;
	private Map<String, VarSymbol> vars;

	public ClassOrTraitSymbol(Modifier modifier, String name, Kind kind,
							  List<String> typeParameterTypes,
							  List<Typed> supertraitTypes,
							  Typed superclassType) {
		this.modifier = modifier;
		this.name = name;
		this.kind = kind;

		this.typeParameterTypes = typeParameterTypes != null ? new ArrayList<>(typeParameterTypes) : new ArrayList<>();
		this.supertraitTypes = supertraitTypes != null ? new ArrayList<>(supertraitTypes) : new ArrayList<>();
		this.superclassType = superclassType;

		this.typeParameters = new ArrayList<>();
		this.supertraits = new ArrayList<>();
		this.superclass = null;
		this.members = new HashMap<>();
		this.funs = new HashMap<>();
		this.vars = new HashMap<>();
	}

	public Modifier getModifier() {
		return modifier;
	}

	public String getName() {
		return name;
	}

	public Kind getKind() {
		return kind;
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

	public boolean isClass() {
		return kind.isClass();
	}

	public boolean isTrait() {
		return kind.isTrait();
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

	public List<Symbol> getSupertraits() {
		return Collections.unmodifiableList(supertraits);
	}

	public void setSupertraits(List<Symbol> traits) {
		this.supertraits.clear();
		if (traits != null) {
			for (Symbol trait : traits) {
				if (trait != null) {
					if (this.supertraits.contains(trait)) continue;

					if (trait.isClassOrTrait()) {
						ClassOrTraitSymbol ct = trait.asClassOrTrait();
						if (ct != null && ct.isTrait()) {
							this.supertraits.add(trait);
						}
					} else if (trait.isParameterized()) {
						ParameterizedClassOrTraitSymbol p = trait.asParameterized();
						if (p != null && p.getBase().isTrait()) {
							this.supertraits.add(trait);
						}
					}
				}
			}
		}
	}

	public boolean hasSupertraits() {
		return !supertraits.isEmpty();
	}

	public Symbol getSuperclass() {
		return superclass;
	}

	public void setSuperclass(Symbol superclass) {
		if (superclass != null) {
			ClassOrTraitSymbol ct = superclass.asClassOrTrait();
			if (ct == null || !ct.isClass()) {
				superclass = null;
			}
		}
		this.superclass = superclass;
	}

	public boolean hasSuperclass() {
		return superclass != null;
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

		if (superclass != null) {
			ClassOrTraitSymbol ct = superclass.asClassOrTrait();
			if (ct == null) {
				ParameterizedClassOrTraitSymbol p = superclass.asParameterized();
				if (p != null) ct = p.getBase();
			}
			if (ct != null) {
				member = ct.getMember(name);
				if (member != null) return member;
			}
		}

		if (supertraits != null) {
			for (Symbol trait : supertraits) {
				ClassOrTraitSymbol ct = trait.asClassOrTrait();
				if (ct == null) {
					ParameterizedClassOrTraitSymbol p = trait.asParameterized();
					if (p != null) ct = p.getBase();
				}
				if (ct != null) {
					member = ct.getMember(name);
					if (member != null) return member;
				}
			}
		}

		return null;
	}

	public FunSymbol getFun(String name) {
		FunSymbol fun = funs.get(name);
		if (fun != null) return fun;

		if (superclass != null) {
			ClassOrTraitSymbol ct = superclass.asClassOrTrait();
			if (ct == null) {
				ParameterizedClassOrTraitSymbol p = superclass.asParameterized();
				if (p != null) ct = p.getBase();
			}
			if (ct != null) {
				fun = ct.getFun(name);
				if (fun != null) return fun;
			}
		}

		if (supertraits != null) {
			for (Symbol trait : supertraits) {
				ClassOrTraitSymbol ct = trait.asClassOrTrait();
				if (ct == null) {
					ParameterizedClassOrTraitSymbol p = trait.asParameterized();
					if (p != null) ct = p.getBase();
				}
				if (ct != null) {
					fun = ct.getFun(name);
					if (fun != null) return fun;
				}
			}
		}

		return null;
	}

	public VarSymbol getVar(String name) {
		VarSymbol var = vars.get(name);
		if (var != null) return var;

		if (superclass != null) {
			ClassOrTraitSymbol ct = superclass.asClassOrTrait();
			if (ct == null) {
				ParameterizedClassOrTraitSymbol p = superclass.asParameterized();
				if (p != null) ct = p.getBase();
			}
			if (ct != null) {
				var = ct.getVar(name);
				if (var != null) return var;
			}
		}

		if (supertraits != null) {
			for (Symbol trait : supertraits) {
				ClassOrTraitSymbol ct = trait.asClassOrTrait();
				if (ct == null) {
					ParameterizedClassOrTraitSymbol p = trait.asParameterized();
					if (p != null) ct = p.getBase();
				}
				if (ct != null) {
					var = ct.getVar(name);
					if (var != null) return var;
				}
			}
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