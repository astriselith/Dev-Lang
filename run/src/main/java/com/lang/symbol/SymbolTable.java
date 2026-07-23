package com.lang.symbol;

import com.lang.ast.Typed;
import com.lang.ast.RefTyped;
import com.lang.ast.ParameterizedRefTyped;
import java.util.*;

public class SymbolTable {
	private final Map<String, ClassSymbol> classes = new LinkedHashMap<>();

	private final Map<String, Symbol> globals = new LinkedHashMap<>();
	private final Map<String, FunSymbol> funs = new LinkedHashMap<>();
	private final Map<String, VarSymbol> vars = new LinkedHashMap<>();

	public SymbolTable() {
	}

	public void register(String name, ClassSymbol cs) {
		classes.put(name, cs);
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

	public Symbol lookup(Symbol left, String name) {
		if (left == null) {
			return getGlobal(name);
		}

		if (left.isClass()) {
			ClassSymbol cls = left.asClass();
			Symbol result = cls.getFun(name);
			if (result != null)
				return result;
			return cls.getVar(name);
		}

		if (left.isParameterized()) {
			ParameterizedClassSymbol param = left.asParameterized();
			ClassSymbol base = param.getBase();
			Symbol result = base.getFun(name);
			if (result != null)
				return result;
			return base.getVar(name);
		}

		return null;
	}

	public Symbol resolve(Typed type) {
		return resolve(type, new HashMap<>());
	}

	public Symbol resolve(Typed type, Map<String, Symbol> substitutions) {
		if (type == null) {
			return null;
		}

		if (type.isRef()) {
			RefTyped ref = (RefTyped) type;
			String name = ref.name.source;

			if (substitutions != null && substitutions.containsKey(name)) {
				return substitutions.get(name);
			}

			return this.get(name);
		}

		if (type.isParameterizedRef()) {
			ParameterizedRefTyped param = (ParameterizedRefTyped) type;
			String name = param.name.source;

			ClassSymbol baseClass = this.get(name);
			if (baseClass == null) {
				return null;
			}

			List<String> typeParameterTypes = baseClass.getTypeParameterTypes();

			if (typeParameterTypes.size() != param.typeArguments.size()) {
				return baseClass;
			}

			Map<String, Symbol> typeArgs = new LinkedHashMap<>();
			for (int i = 0; i < typeParameterTypes.size(); i++) {
				String paramName = typeParameterTypes.get(i);
				Typed arg = param.typeArguments.get(i);

				Symbol resolvedArg = resolve(arg, substitutions);
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

	public boolean isSameType(Symbol sym1, Symbol sym2) {
		if (sym1 == null && sym2 == null)
			return true;
		if (sym1 == null || sym2 == null)
			return false;
		if (sym1 == sym2)
			return true;

		if (sym1.isTypeParam() && sym2.isTypeParam()) {
			return sym1 == sym2;
		}

		if (sym1.isClass() && sym2.isClass()) {
			return sym1 == sym2;
		}

		if (sym1.isParameterized() && sym2.isParameterized()) {
			ParameterizedClassSymbol p1 = sym1.asParameterized();
			ParameterizedClassSymbol p2 = sym2.asParameterized();

			if (p1.getBase() != p2.getBase()) {
				return false;
			}

			Map<String, Symbol> args1 = p1.getTypeArguments();
			Map<String, Symbol> args2 = p2.getTypeArguments();

			if (args1.size() != args2.size())
				return false;

			for (Map.Entry<String, Symbol> entry : args1.entrySet()) {
				String key = entry.getKey();
				if (!args2.containsKey(key))
					return false;
				if (!isSameType(entry.getValue(), args2.get(key)))
					return false;
			}

			return true;
		}

		return false;
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

	public boolean isNumeric(Symbol sym) {
		if (sym == null)
			return false;
		return isSubtype(sym, getNumber());
	}

	public boolean isSubtype(Symbol sub, Symbol sup) {
		if (sub == null || sup == null)
			return false;
		if (sub == sup)
			return true;
		if (isSameType(sub, sup))
			return true;

		if (sub.isTypeParam() && sup.isTypeParam()) {
			return sub == sup;
		}

		ClassSymbol any = getAny();

		if (sub.isTypeParam() && sup.isClass()) {
			TypeParamSymbol tp = sub.asTypeParam();

			for (Symbol trait : tp.getSuperclasses()) {
				if (isSubtype(trait, sup)) {
					return true;
				}
			}

			return sup == any;
		}

		if (sub.isClass() && sup.isTypeParam()) {
			return sub == any;
		}

		if (sub.isParameterized() && sup.isParameterized()) {
			ParameterizedClassSymbol pSub = sub.asParameterized();
			ParameterizedClassSymbol pSup = sup.asParameterized();

			if (pSub.getBase() == pSup.getBase()) {
				Map<String, Symbol> argsSub = pSub.getTypeArguments();
				Map<String, Symbol> argsSup = pSup.getTypeArguments();

				if (argsSub.size() != argsSup.size())
					return false;

				for (Map.Entry<String, Symbol> entry : argsSub.entrySet()) {
					String key = entry.getKey();
					if (!argsSup.containsKey(key))
						return false;
					if (!isSubtype(entry.getValue(), argsSup.get(key)))
						return false;
				}
				return true;
			}
		}

		if ((sub.isClass() || sub.isParameterized()) &&
				(sup.isClass() || sup.isParameterized())) {

			Set<Symbol> visited = new HashSet<>();
			Deque<Symbol> stack = new ArrayDeque<>();
			stack.push(sub);

			while (!stack.isEmpty()) {
				Symbol current = stack.pop();
				if (current == null)
					continue;
				if (visited.contains(current))
					continue;
				visited.add(current);

				if (isSameType(current, sup)) {
					return true;
				}

				ClassSymbol currentBase = current.isParameterized()
						? current.asParameterized().getBase()
						: current.isClass() ? current.asClass() : null;

				if (currentBase == null)
					continue;

				for (Symbol trait : currentBase.getSuperclasses()) {
					if (trait != null && !visited.contains(trait)) {
						stack.push(trait);
					}
				}
			}
			return false;
		}

		if (isNull(sub)) {
			return !isVoid(sup);
		}

		if (sup == getNumber() && (sub == getInt() || sub == getFloat())) {
			return true;
		}

		if (sup == any) {
			return true;
		}

		return false;
	}

	public boolean isAssignable(Symbol source, Symbol target) {
		if (source == null || target == null)
			return false;
		if (source == target)
			return true;
		if (isSameType(source, target))
			return true;

		if (isNull(source)) {
			return !isVoid(target);
		}

		return isSubtype(source, target);
	}

	public Symbol getCommonSupertype(Symbol a, Symbol b) {
		if (a == null)
			return b;
		if (b == null)
			return a;

		if (isSameType(a, b))
			return a;
		if (isSubtype(a, b))
			return b;
		if (isSubtype(b, a))
			return a;

		if (isNumeric(a) && isNumeric(b)) {
			return getNumber();
		}

		return getAny();
	}

	public Symbol getArithmeticResultType(Symbol a, Symbol b) {
		if (!isNumeric(a) || !isNumeric(b))
			return null;

		boolean hasFloat = (a != null && isSameType(a, getFloat())) ||
				(b != null && isSameType(b, getFloat()));

		if (hasFloat) {
			return getFloat();
		}

		return getInt();
	}
}