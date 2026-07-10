package com.dev.lang.semantic;

import com.dev.lang.ast.*;
import com.dev.lang.symbol.*;
import java.util.*;

public class TypeChecker {
	private final SymbolTable symbolTable;
	private final TypeResolver resolver;

	public TypeChecker(SymbolTable symbolTable) {
		this.symbolTable = symbolTable;
		this.resolver = new TypeResolver(symbolTable);
	}

	private void log(String message) {
		System.out.println("[TypeChecker] " + message);
	}

	public boolean isSameType(Typed type1, Typed type2, Scope scope) {
		log("isSameType: type1=" + type1 + ", type2=" + type2);
		if (type1 == null && type2 == null) return true;
		if (type1 == null || type2 == null) return false;

		Symbol sym1 = resolver.resolve(type1, scope);
		Symbol sym2 = resolver.resolve(type2, scope);
		log("  resolved sym1=" + (sym1 != null ? sym1.name : "null") + ", sym2=" + (sym2 != null ? sym2.name : "null"));

		return isSameType(sym1, sym2);
	}

	public boolean isSameType(Symbol sym1, Symbol sym2) {
		log("isSameType(sym): sym1=" + (sym1 != null ? sym1.name : "null") + ", sym2=" + (sym2 != null ? sym2.name : "null"));
		if (sym1 == null && sym2 == null) return true;
		if (sym1 == null || sym2 == null) return false;
		if (sym1 == sym2) return true;

		if (sym1.isTypeParam() && sym2.isTypeParam()) {
			boolean result = sym1.name.equals(sym2.name);
			log("  both type params, result=" + result);
			return result;
		}

		if (sym1.isClassOrTrait() && sym2.isClassOrTrait()) {
			boolean result = sym1.name.equals(sym2.name);
			log("  both class/trait, result=" + result);
			return result;
		}

		log("  result=false");
		return false;
	}

	public boolean isSubtype(Typed sub, Typed sup, Scope scope) {
		log("isSubtype: sub=" + sub + ", sup=" + sup);
		if (sub == null || sup == null) return false;

		Symbol subSym = resolver.resolve(sub, scope);
		Symbol supSym = resolver.resolve(sup, scope);
		log("  resolved subSym=" + (subSym != null ? subSym.name : "null") + ", supSym=" + (supSym != null ? supSym.name : "null"));

		return isSubtype(subSym, supSym, scope);
	}

	public boolean isSubtype(Symbol sub, Symbol sup, Scope scope) {
		log("isSubtype(sym): sub=" + (sub != null ? sub.name : "null") + ", sup=" + (sup != null ? sup.name : "null"));
		if (sub == null || sup == null) return false;
		if (sub == sup) return true;
		if (isSameType(sub, sup)) return true;

		if (sub.isTypeParam() && sup.isTypeParam()) {
			boolean result = sub.name.equals(sup.name);
			log("  both type params, result=" + result);
			return result;
		}

		if (sub.isTypeParam() && sup.isClassOrTrait()) {
			Symbol any = resolver.resolve(new RefTyped("Any", null), scope);
			boolean result = any != null && sup == any;
			log("  sub type param, sup class/trait, any=" + (any != null ? any.name : "null") + ", result=" + result);
			return result;
		}

		if (sub.isClassOrTrait() && sup.isTypeParam()) {
			Symbol any = resolver.resolve(new RefTyped("Any", null), scope);
			boolean result = any != null && sub == any;
			log("  sub class/trait, sup type param, any=" + (any != null ? any.name : "null") + ", result=" + result);
			return result;
		}

		if (sub.isClassOrTrait() && sup.isClassOrTrait()) {
			ClassOrTraitSymbol subClass = (ClassOrTraitSymbol) sub;
			ClassOrTraitSymbol supClass = (ClassOrTraitSymbol) sup;
			log("  both class/trait, checking hierarchy from " + subClass.name + " to " + supClass.name);

			Set<String> visited = new HashSet<>();
			Deque<ClassOrTraitSymbol> stack = new ArrayDeque<>();
			stack.push(subClass);

			while (!stack.isEmpty()) {
				ClassOrTraitSymbol current = stack.pop();
				if (current == null) continue;
				if (visited.contains(current.name)) continue;
				visited.add(current.name);
				log("    checking: " + current.name);

				if (current.name.equals(supClass.name)) {
					log("    found match!");
					return true;
				}

				for (ClassOrTraitSymbol trait : current.getSupertraits()) {
					if (trait != null && !visited.contains(trait.name)) {
						log("      adding supertrait: " + trait.name);
						stack.push(trait);
					}
				}

				if (current.hasSuperclass()) {
					ClassOrTraitSymbol superclass = current.getSuperclass();
					if (superclass != null && !visited.contains(superclass.name)) {
						log("      adding superclass: " + superclass.name);
						stack.push(superclass);
					}
				}
			}
			log("  no hierarchy path found");
			return false;
		}

		Symbol any = resolver.resolve(new RefTyped("Any", null), scope);
		if (any != null && sup == any) {
			log("  sup is Any, result=true");
			return true;
		}

		if (sub.name.equals("Null")) {
			if (sup.name.equals("Void")) {
				log("  Null to Void, result=false");
				return false;
			}
			log("  Null to non-Void, result=true");
			return true;
		}

		Symbol number = resolver.resolve(new RefTyped("Number", null), scope);
		if (sup == number && (sub.name.equals("Int") || sub.name.equals("Float"))) {
			log("  numeric promotion to Number, result=true");
			return true;
		}

		log("  final result=false");
		return false;
	}

	public boolean isAssignable(Typed source, Typed target, Scope scope) {
		log("isAssignable: source=" + source + ", target=" + target);
		if (source == null || target == null) return false;

		Symbol sourceSym = resolver.resolve(source, scope);
		Symbol targetSym = resolver.resolve(target, scope);
		log("  resolved sourceSym=" + (sourceSym != null ? sourceSym.name : "null") + ", targetSym=" + (targetSym != null ? targetSym.name : "null"));

		if (sourceSym != null && targetSym != null) {
			return isAssignable(sourceSym, targetSym, scope);
		}

		log("  checking parameterized cases");

		if (source instanceof ParameterizedRefTyped && target instanceof RefTyped) {
			ParameterizedRefTyped paramSource = (ParameterizedRefTyped) source;
			RefTyped rawTarget = (RefTyped) target;
			log("  source is ParameterizedRefTyped with name=" + paramSource.name + ", target is RefTyped with name=" + rawTarget.name);

			Symbol sourceRawSym = resolver.resolve(new RefTyped(paramSource.name, null), scope);
			Symbol targetSymResolved = resolver.resolve(rawTarget, scope);
			log("  sourceRawSym=" + (sourceRawSym != null ? sourceRawSym.name : "null") + ", targetSymResolved=" + (targetSymResolved != null ? targetSymResolved.name : "null"));

			if (sourceRawSym != null && targetSymResolved != null) {
				boolean result = isSameType(sourceRawSym, targetSymResolved);
				log("  ParameterizedType -> RawType result=" + result);
				return result;
			}
		}

		if (source instanceof RefTyped && target instanceof ParameterizedRefTyped) {
			ParameterizedRefTyped paramTarget = (ParameterizedRefTyped) target;
			RefTyped rawSource = (RefTyped) source;
			log("  source is RefTyped with name=" + rawSource.name + ", target is ParameterizedRefTyped with name=" + paramTarget.name);

			Symbol sourceRawSym = resolver.resolve(rawSource, scope);
			Symbol targetRawSym = resolver.resolve(new RefTyped(paramTarget.name, null), scope);
			log("  sourceRawSym=" + (sourceRawSym != null ? sourceRawSym.name : "null") + ", targetRawSym=" + (targetRawSym != null ? targetRawSym.name : "null"));

			if (sourceRawSym != null && targetRawSym != null &&
					isSameType(sourceRawSym, targetRawSym)) {
				log("  RawType -> ParameterizedType not allowed, returning false");
				return false;
			}
		}

		log("  result=false");
		return false;
	}

	public boolean isAssignable(Symbol source, Symbol target, Scope scope) {
		log("isAssignable(sym): source=" + (source != null ? source.name : "null") + ", target=" + (target != null ? target.name : "null"));
		if (source == null || target == null) return false;
		if (source == target) return true;
		if (isSameType(source, target)) return true;

		if (source.name.equals("Null")) {
			boolean result = !target.name.equals("Void");
			log("  Null check, result=" + result);
			return result;
		}

		boolean result = isSubtype(source, target, scope);
		log("  isSubtype result=" + result);
		return result;
	}

	public boolean isNumeric(Typed type, Scope scope) {
		log("isNumeric: type=" + type);
		if (type == null) return false;
		Symbol sym = resolver.resolve(type, scope);
		boolean result = isNumeric(sym, scope);
		log("  result=" + result);
		return result;
	}

	public boolean isNumeric(Symbol sym, Scope scope) {
		if (sym == null) return false;

		Symbol number = resolver.resolve(new RefTyped("Number", null), scope);
		if (number == null) return false;

		return isSubtype(sym, number, scope);
	}

	public boolean isBoolean(Typed type, Scope scope) {
		if (type == null) return false;
		Symbol sym = resolver.resolve(type, scope);
		return isBoolean(sym, scope);
	}

	public boolean isBoolean(Symbol sym, Scope scope) {
		if (sym == null) return false;
		Symbol bool = resolver.resolve(new RefTyped("Boolean", null), scope);
		return bool != null && isSameType(sym, bool);
	}

	public boolean isString(Typed type, Scope scope) {
		if (type == null) return false;
		Symbol sym = resolver.resolve(type, scope);
		return isString(sym, scope);
	}

	public boolean isString(Symbol sym, Scope scope) {
		if (sym == null) return false;
		Symbol str = resolver.resolve(new RefTyped("String", null), scope);
		return str != null && isSameType(sym, str);
	}

	public boolean isVoid(Typed type, Scope scope) {
		if (type == null) return false;
		Symbol sym = resolver.resolve(type, scope);
		return isVoid(sym, scope);
	}

	public boolean isVoid(Symbol sym, Scope scope) {
		if (sym == null) return false;
		Symbol v = resolver.resolve(new RefTyped("Void", null), scope);
		return v != null && isSameType(sym, v);
	}

	public Typed getCommonSupertype(Typed a, Typed b, Scope scope) {
		log("getCommonSupertype: a=" + a + ", b=" + b);
		if (a == null) return b;
		if (b == null) return a;

		Symbol aSym = resolver.resolve(a, scope);
		Symbol bSym = resolver.resolve(b, scope);

		if (aSym == null) return b;
		if (bSym == null) return a;

		if (isSameType(aSym, bSym)) return a;
		if (isSubtype(aSym, bSym, scope)) return b;
		if (isSubtype(bSym, aSym, scope)) return a;

		if (isNumeric(aSym, scope) && isNumeric(bSym, scope)) {
			log("  common supertype: Number");
			return new RefTyped("Number", null);
		}

		Symbol any = resolver.resolve(new RefTyped("Any", null), scope);
		if (any != null) {
			log("  common supertype: Any");
			return new RefTyped(any.name, null);
		}

		log("  common supertype: null");
		return null;
	}

	public Typed getArithmeticResultType(Typed a, Typed b, Scope scope) {
		log("getArithmeticResultType: a=" + a + ", b=" + b);
		if (!isNumeric(a, scope) || !isNumeric(b, scope)) return null;

		Symbol aSym = resolver.resolve(a, scope);
		Symbol bSym = resolver.resolve(b, scope);

		Symbol floatClass = resolver.resolve(new RefTyped("Float", null), scope);

		boolean hasFloat = (aSym != null && isSameType(aSym, floatClass)) ||
						   (bSym != null && isSameType(bSym, floatClass));

		if (hasFloat) {
			log("  result: Float");
			return new RefTyped("Float", null);
		}

		log("  result: Int");
		return new RefTyped("Int", null);
	}

	public boolean isApplicable(FunSymbol function, List<Typed> argTypes, Scope scope) {
		log("isApplicable: function=" + (function != null ? function.name : "null") + ", argTypes=" + argTypes);
		if (function == null) return false;
		if (function.getArity() != argTypes.size()) return false;

		List<Typed> paramTypes = function.getParameterTypes();
		for (int i = 0; i < function.getArity(); i++) {
			log("  checking arg[" + i + "]=" + argTypes.get(i) + " -> param[" + i + "]=" + paramTypes.get(i));
			if (!isAssignable(argTypes.get(i), paramTypes.get(i), scope)) {
				log("  not assignable at index " + i);
				return false;
			}
		}
		log("  function is applicable");
		return true;
	}

	public FunSymbol findFunction(Scope scope, String name) {
		Symbol sym = scope.resolve(name);
		if (sym != null && sym.isFun()) {
			return (FunSymbol) sym;
		}
		return null;
	}

	public FunSymbol findFunction(ClassOrTraitSymbol cls, String name, Scope scope) {
		if (cls == null) return null;
		return cls.getFun(name);
	}

	public boolean canOverride(FunSymbol override, FunSymbol base, Scope scope) {
		if (override == null || base == null) return false;
		if (!override.name.equals(base.name)) return false;
		if (override.getArity() != base.getArity()) return false;

		List<Typed> overrideParams = override.getParameterTypes();
		List<Typed> baseParams = base.getParameterTypes();

		for (int i = 0; i < override.getArity(); i++) {
			if (!isSameType(overrideParams.get(i), baseParams.get(i), scope)) {
				return false;
			}
		}

		return isSubtype(override.getReturnType(), base.getReturnType(), scope);
	}
}