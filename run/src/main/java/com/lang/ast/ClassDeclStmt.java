package com.lang.ast;

import com.lang.util.Position;
import java.util.ArrayList;
import java.util.List;

public class ClassDeclStmt extends Stmt {
	public final String name;

	public final Typed superclass;
	public final List<Typed> supertraits;
	public final List<TypeParamDeclStmt> typeParameters;

	public final List<FunDeclStmt> funs;
	public final List<VarDeclStmt> vars;

	public ClassDeclStmt(String name, Typed superclass,
			List<Typed> supertraits, List<TypeParamDeclStmt> typeParameters,
			List<FunDeclStmt> funs, List<VarDeclStmt> vars, Position position) {
		super(position);
		this.name = name;

		this.superclass = superclass;
		this.supertraits = supertraits != null ? supertraits : new ArrayList<>();
		this.typeParameters = typeParameters != null ? typeParameters : new ArrayList<>();
		this.funs = funs != null ? funs : new ArrayList<>();
		this.vars = vars != null ? vars : new ArrayList<>();
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
}