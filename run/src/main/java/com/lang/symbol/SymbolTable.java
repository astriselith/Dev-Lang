package com.lang.symbol;

import com.lang.ast.Typed;
import com.lang.ast.RefTyped;
import com.lang.ast.ParameterizedRefTyped;
import java.util.*;
import java.util.function.Consumer;

public class SymbolTable {
	private final Map<String, ClassSymbol> classes = new LinkedHashMap<>();

	private final Map<String, Symbol> globals = new LinkedHashMap<>();
	private final Map<String, FunSymbol> funs = new LinkedHashMap<>();
	private final Map<String, VarSymbol> vars = new LinkedHashMap<>();

	public SymbolTable() {
	}

	public void add(ClassSymbol cs) {
		classes.put(cs.getName(), cs);
	}

	public ClassSymbol get(String name) {
		return classes.get(name);
	}

	public boolean has(String name) {
		return classes.containsKey(name);
	}

	public Map<String, ClassSymbol> getAll() {
		return Collections.unmodifiableMap(classes);
	}

	public void addGlobal(String name, Symbol sym) {
		globals.put(name, sym);
		if (sym.isFun()) {
			funs.put(name, sym.asFun());
		} else if (sym.isVar()) {
			vars.put(name, sym.asVar());
		}
	}

	public Symbol getGlobal(String name) {
		return globals.get(name);
	}

	public boolean hasGlobal(String name) {
		return globals.containsKey(name);
	}

	public Map<String, Symbol> getGlobals() {
		return Collections.unmodifiableMap(globals);
	}

	public FunSymbol getFun(String name) {
		return funs.get(name);
	}

	public boolean hasFun(String name) {
		return funs.containsKey(name);
	}

	public Map<String, FunSymbol> getFuns() {
		return Collections.unmodifiableMap(funs);
	}

	public VarSymbol getVar(String name) {
		return vars.get(name);
	}

	public boolean hasVar(String name) {
		return vars.containsKey(name);
	}

	public Map<String, VarSymbol> getVars() {
		return Collections.unmodifiableMap(vars);
	}

	public Symbol resolve(Typed type) {
		return resolve(type, new HashMap<>(), null);
	}

	public Symbol resolve(Typed type, Map<String, Symbol> substitutions, Consumer<Typed> report) {
		if (type == null) {
			return null;
		}

		if (type.isRef()) {
			RefTyped ref = (RefTyped) type;
			String name = ref.name.source;

			if (substitutions != null && substitutions.containsKey(name)) {
				return substitutions.get(name);
			}

			Symbol symbol = this.get(name);

			if (symbol != null) {
				return symbol;
			}

			if (report != null) {
				report.accept(ref);
			}

			return null;
		}

		if (type.isParameterizedRef()) {
			ParameterizedRefTyped param = (ParameterizedRefTyped) type;
			String name = param.name.source;

			Symbol symbol = this.get(name);

			if (!(symbol instanceof ClassSymbol baseClass)) {
				if (report != null) {
					report.accept(param);
				}

				if (param.typeArguments != null) {
					param.typeArguments.forEach(t -> resolve(t, substitutions, report));
				}

				return null;
			}

			Map<String, TypeParamSymbol> typeParameters = baseClass.getTypeParameters();

			if (typeParameters.size() != param.typeArguments.size()) {
				if (report != null) {
					report.accept(param);
				}
				return null;
			}

			Map<String, Symbol> typeArgs = new LinkedHashMap<>();

			int index = 0;
			for (String paramName : typeParameters.keySet()) {
				Typed arg = param.typeArguments.get(index++);

				Symbol resolvedArg = resolve(arg, substitutions, report);

				if (resolvedArg != null) {
					typeArgs.put(paramName, resolvedArg);
				}
			}

			return new ParameterizedClassSymbol(baseClass, typeArgs);
		}

		return null;
	}

	public ClassSymbol getAny() {
		return get("Any");
	}

	public ClassSymbol getNumber() {
		return get("Number");
	}

	public ClassSymbol getInt() {
		return get("Int");
	}

	public ClassSymbol getFloat() {
		return get("Float");
	}

	public ClassSymbol getBool() {
		return get("Bool");
	}

	public ClassSymbol getString() {
		return get("String");
	}

	public ClassSymbol getVoid() {
		return get("Void");
	}

	public ClassSymbol getNull() {
		return get("Null");
	}

	public boolean isBool(Symbol sym) {
		return sym != null && sym == getBool();
	}

	public boolean isString(Symbol sym) {
		return sym != null && sym == getString();
	}

	public boolean isVoid(Symbol sym) {
		return sym != null && sym == getVoid();
	}

	public boolean isNull(Symbol sym) {
		return sym != null && sym == getNull();
	}

	public boolean isNumber(Symbol sym) {
		return sym != null && sym == getNumber();
	}

	public boolean isInt(Symbol sym) {
		return sym != null && sym == getInt();
	}

	public boolean isFloat(Symbol sym) {
		return sym != null && sym == getFloat();
	}

	public boolean isAny(Symbol sym) {
		return sym != null && sym == getAny();
	}
}