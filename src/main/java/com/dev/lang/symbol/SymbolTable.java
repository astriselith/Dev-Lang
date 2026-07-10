package com.dev.lang.symbol;

import com.dev.lang.ast.Typed;
import com.dev.lang.ast.RefTyped;
import com.dev.lang.ast.ParameterizedRefTyped;
import java.util.*;

public class SymbolTable {
	private final Map<String, ClassOrTraitSymbol> classesAndTraits = new LinkedHashMap<>();
	private final Map<String, Symbol> globals = new LinkedHashMap<>();
	private final Map<String, FunSymbol> funs = new LinkedHashMap<>();
	private final Map<String, VarSymbol> vars = new LinkedHashMap<>();

	public SymbolTable() {
	}

	public void register(String name, ClassOrTraitSymbol cs) {
		classesAndTraits.put(name, cs);
	}

	public ClassOrTraitSymbol get(String name) {
		return classesAndTraits.get(name);
	}

	public boolean has(String name) {
		return classesAndTraits.containsKey(name);
	}

	public Map<String, ClassOrTraitSymbol> getAll() {
		return Collections.unmodifiableMap(classesAndTraits);
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

		if (left.isClassOrTrait()) {
			ClassOrTraitSymbol cls = left.asClassOrTrait();
			Symbol result = cls.getFun(name);
			if (result != null) return result;
			return cls.getVar(name);
		}

		if (left.isParameterized()) {
			ParameterizedClassOrTraitSymbol param = left.asParameterized();
			ClassOrTraitSymbol base = param.getBase();
			Symbol result = base.getFun(name);
			if (result != null) return result;
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
			String name = ref.name;

			if (substitutions != null && substitutions.containsKey(name)) {
				return substitutions.get(name);
			}

			return this.get(name);
		}

		if (type.isParameterizedRef()) {
			ParameterizedRefTyped param = (ParameterizedRefTyped) type;
			String name = param.name;

			ClassOrTraitSymbol baseClass = this.get(name);
			if (baseClass == null) {
				return null;
			}

			List<String> typeParamNames = baseClass.getTypeParameterTypes();

			if (typeParamNames.size() != param.typeArguments.size()) {
				return baseClass;
			}

			Map<String, Symbol> typeArgs = new LinkedHashMap<>();
			for (int i = 0; i < typeParamNames.size(); i++) {
				String paramName = typeParamNames.get(i);
				Typed arg = param.typeArguments.get(i);

				Symbol resolvedArg = resolve(arg, substitutions);
				if (resolvedArg != null) {
					typeArgs.put(paramName, resolvedArg);
				}
			}

			return new ParameterizedClassOrTraitSymbol(baseClass, typeArgs);
		}

		return null;
	}

	public ClassOrTraitSymbol getAny() {
		return get("Any");
	}

	public ClassOrTraitSymbol getNumber() {
		return get("Number");
	}

	public ClassOrTraitSymbol getInt() {
		return get("Int");
	}

	public ClassOrTraitSymbol getFloat() {
		return get("Float");
	}

	public ClassOrTraitSymbol getBool() {
		return get("Bool");
	}

	public ClassOrTraitSymbol getString() {
		return get("String");
	}

	public ClassOrTraitSymbol getVoid() {
		return get("Void");
	}

	public ClassOrTraitSymbol getNull() {
		return get("Null");
	}

	public boolean isSameType(Symbol sym1, Symbol sym2) {
		if (sym1 == null && sym2 == null) return true;
		if (sym1 == null || sym2 == null) return false;
		if (sym1 == sym2) return true;

		if (sym1.isTypeParam() && sym2.isTypeParam()) {
			return sym1 == sym2;
		}

		if (sym1.isClassOrTrait() && sym2.isClassOrTrait()) {
			return sym1 == sym2;
		}

		if (sym1.isParameterized() && sym2.isParameterized()) {
			ParameterizedClassOrTraitSymbol p1 = sym1.asParameterized();
			ParameterizedClassOrTraitSymbol p2 = sym2.asParameterized();

			if (p1.getBase() != p2.getBase()) {
				return false;
			}

			Map<String, Symbol> args1 = p1.getTypeArguments();
			Map<String, Symbol> args2 = p2.getTypeArguments();

			if (args1.size() != args2.size()) return false;

			for (Map.Entry<String, Symbol> entry : args1.entrySet()) {
				String key = entry.getKey();
				if (!args2.containsKey(key)) return false;
				if (!isSameType(entry.getValue(), args2.get(key))) return false;
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
		if (sym == null) return false;
		return isSubtype(sym, getNumber());
	}

	public boolean isSubtype(Symbol sub, Symbol sup) {
		if (sub == null || sup == null) return false;
		if (sub == sup) return true;
		if (isSameType(sub, sup)) return true;

		if (sub.isTypeParam() && sup.isTypeParam()) {
			return sub == sup;
		}

		ClassOrTraitSymbol any = getAny();

		if (sub.isTypeParam() && sup.isClassOrTrait()) {
			TypeParamSymbol tp = sub.asTypeParam();

			Symbol superclass = tp.getSuperclass();
			if (superclass != null && isSubtype(superclass, sup)) {
				return true;
			}

			for (Symbol trait : tp.getSupertraits()) {
				if (isSubtype(trait, sup)) {
					return true;
				}
			}

			return sup == any;
		}

		if (sub.isClassOrTrait() && sup.isTypeParam()) {
			return sub == any;
		}

		if (sub.isParameterized() && sup.isParameterized()) {
			ParameterizedClassOrTraitSymbol pSub = sub.asParameterized();
			ParameterizedClassOrTraitSymbol pSup = sup.asParameterized();

			if (pSub.getBase() == pSup.getBase()) {
				Map<String, Symbol> argsSub = pSub.getTypeArguments();
				Map<String, Symbol> argsSup = pSup.getTypeArguments();

				if (argsSub.size() != argsSup.size()) return false;

				for (Map.Entry<String, Symbol> entry : argsSub.entrySet()) {
					String key = entry.getKey();
					if (!argsSup.containsKey(key)) return false;
					if (!isSubtype(entry.getValue(), argsSup.get(key))) return false;
				}
				return true;
			}
		}

		if ((sub.isClassOrTrait() || sub.isParameterized()) &&
				(sup.isClassOrTrait() || sup.isParameterized())) {

			Set<Symbol> visited = new HashSet<>();
			Deque<Symbol> stack = new ArrayDeque<>();
			stack.push(sub);

			while (!stack.isEmpty()) {
				Symbol current = stack.pop();
				if (current == null) continue;
				if (visited.contains(current)) continue;
				visited.add(current);

				if (isSameType(current, sup)) {
					return true;
				}

				ClassOrTraitSymbol currentBase = current.isParameterized()
												 ? current.asParameterized().getBase()
												 : current.isClassOrTrait() ? current.asClassOrTrait() : null;

				if (currentBase == null) continue;

				for (Symbol trait : currentBase.getSupertraits()) {
					if (trait != null && !visited.contains(trait)) {
						stack.push(trait);
					}
				}

				if (currentBase.hasSuperclass()) {
					Symbol superclass = currentBase.getSuperclass();
					if (superclass != null && !visited.contains(superclass)) {
						stack.push(superclass);
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
		if (source == null || target == null) return false;
		if (source == target) return true;
		if (isSameType(source, target)) return true;

		if (isNull(source)) {
			return !isVoid(target);
		}

		return isSubtype(source, target);
	}

	public Symbol getCommonSupertype(Symbol a, Symbol b) {
		if (a == null) return b;
		if (b == null) return a;

		if (isSameType(a, b)) return a;
		if (isSubtype(a, b)) return b;
		if (isSubtype(b, a)) return a;

		if (isNumeric(a) && isNumeric(b)) {
			return getNumber();
		}

		return getAny();
	}

	public Symbol getArithmeticResultType(Symbol a, Symbol b) {
		if (!isNumeric(a) || !isNumeric(b)) return null;

		boolean hasFloat = (a != null && isSameType(a, getFloat())) ||
						   (b != null && isSameType(b, getFloat()));

		if (hasFloat) {
			return getFloat();
		}

		return getInt();
	}
}