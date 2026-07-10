package com.dev.lang.ast;

import com.dev.lang.util.Position;
import java.util.ArrayList;
import java.util.List;

public class ClassOrTraitDeclStmt extends Stmt {
	public final Modifier modifier;
	public final String name;
	public final Kind kind;

	public final Typed superclass;
	public final List<Typed> supertraits;
	public final List<TypeParamDeclStmt> typeParameters;

	public final List<FunDeclStmt> funs;
	public final List<VarDeclStmt> vars;

	public ClassOrTraitDeclStmt(Modifier modifier, String name, Kind kind, Typed superclass,
								List<Typed> supertraits, List<TypeParamDeclStmt> typeParameters,
								List<FunDeclStmt> funs, List<VarDeclStmt> vars, Position position) {
		super(position);
		this.modifier = modifier != null ? modifier : new Modifier(position);
		this.name = name;
		this.kind = kind != null ? kind : new Kind();

		this.superclass = superclass;
		this.supertraits = supertraits != null ? supertraits : new ArrayList<>();
		this.typeParameters = typeParameters != null ? typeParameters : new ArrayList<>();
		this.funs = funs != null ? funs : new ArrayList<>();
		this.vars = vars != null ? vars : new ArrayList<>();
	}

	public boolean isClass() {
		return kind.isClass();
	}

	public boolean isTrait() {
		return kind.isTrait();
	}

	public boolean hasSuperclass() {
		return superclass != null;
	}

	public boolean hasSupertraits() {
		return !supertraits.isEmpty();
	}

	public boolean hasTypeParameters() {
		return !typeParameters.isEmpty();
	}

	public boolean hasFuns() {
		return !funs.isEmpty();
	}

	public boolean hasVars() {
		return !vars.isEmpty();
	}

	public FunDeclStmt getFun(String name) {
		for (FunDeclStmt fun : funs) {
			if (fun.name.equals(name)) {
				return fun;
			}
		}
		return null;
	}

	public VarDeclStmt getVar(String name) {
		for (VarDeclStmt var : vars) {
			if (var.name.equals(name)) {
				return var;
			}
		}
		return null;
	}

	public boolean hasFun(String name) {
		return getFun(name) != null;
	}

	public boolean hasVar(String name) {
		return getVar(name) != null;
	}

	public boolean hasMember(String name) {
		return hasFun(name) || hasVar(name);
	}
}