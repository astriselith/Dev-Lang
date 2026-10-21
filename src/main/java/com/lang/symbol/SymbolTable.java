package com.lang.symbol;

import com.lang.ast.Typed;
import com.lang.ast.RefTyped;
import com.lang.ast.ParameterizedRefTyped;
import java.util.*;
import java.util.function.Consumer;

public class SymbolTable {
    private final Map<String, ClassSymbol> classes = new LinkedHashMap<>();

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

    public Symbol unwrap(Symbol sym) {
        if (sym == null)
            return null;
        if (sym.isClass() || sym.isParameterizedClass() || sym.isTypeParam())
            return sym;
        if (sym.isMember())
            return sym.asMember().getType();
        if (sym.isParam())
            return sym.asParam().getType();
        if (sym.isBound())
            return sym.asBound().getType();
        return null;
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

    public boolean isSametype(Symbol a, Symbol b) {
        if (a == null || b == null) {
            return a == b;
        }

        if (a == b) {
            return true;
        }

        if (a.isClass() && b.isClass()) {
            return a == b;
        }

        if (a.isParameterizedClass() && b.isParameterizedClass()) {
            ParameterizedClassSymbol pa = a.asParameterizedClass();
            ParameterizedClassSymbol pb = b.asParameterizedClass();

            if (pa.getBase() != pb.getBase()) {
                return false;
            }

            Map<String, Symbol> argsA = pa.getTypeArguments();
            Map<String, Symbol> argsB = pb.getTypeArguments();

            if (argsA.size() != argsB.size()) {
                return false;
            }

            for (String key : argsA.keySet()) {
                if (!isSametype(argsA.get(key), argsB.get(key))) {
                    return false;
                }
            }
            return true;
        }

        if (a.isTypeParam() && b.isTypeParam()) {
            return a == b;
        }

        if (a.isFun() && b.isFun()) {
            FunSymbol fa = a.asFun();
            FunSymbol fb = b.asFun();

            if (!isSametype(fa.getType(), fb.getType())) {
                return false;
            }

            Map<String, ParamSymbol> paramsA = fa.getParameters();
            Map<String, ParamSymbol> paramsB = fb.getParameters();

            if (paramsA.size() != paramsB.size()) {
                return false;
            }

            Iterator<String> itA = paramsA.keySet().iterator();
            Iterator<String> itB = paramsB.keySet().iterator();

            while (itA.hasNext() && itB.hasNext()) {
                String nameA = itA.next();
                String nameB = itB.next();

                if (!isSametype(paramsA.get(nameA).getType(), paramsB.get(nameB).getType())) {
                    return false;
                }
            }
            return true;
        }

        if (a.isVar() && b.isVar()) {
            return isSametype(a.asVar().getType(), b.asVar().getType());
        }

        return false;
    }

    public boolean isAssignable(Symbol from, Symbol to) {
        if (from == null || to == null) {
            return false;
        }

        if (isSametype(from, to)) {
            return true;
        }

        if (isNull(from)) {
            return to.isClass() && !isPrimitive(to);
        }

        if (isAny(from) && isAny(to)) {
            return true;
        }

        if (isNumber(from) && isNumber(to)) {
            return true;
        }

        return isSubtype(from, to);
    }

    public boolean isSubtype(Symbol subType, Symbol superType) {
        if (subType == null || superType == null) {
            return false;
        }

        if (subType == superType) {
            return true;
        }

        if (isAny(superType)) {
            return true;
        }

        if (isNull(subType)) {
            return superType.isClass() && !isPrimitive(superType);
        }

        if (superType.isTypeParam()) {
            TypeParamSymbol tp = superType.asTypeParam();
            for (BoundSymbol bound : tp.getBounds().values()) {
                Symbol boundType = bound.getType();
                if (boundType != null && isSubtype(subType, boundType)) {
                    return true;
                }
            }
            return false;
        }

        if (subType.isClass()) {
            ClassSymbol subClass = subType.asClass();
            for (SuperSymbol parent : subClass.getSupers().values()) {
                Symbol parentType = parent.getType();
                if (parentType == null)
                    continue;

                if (parentType == superType) {
                    return true;
                }
                if (isSubtype(parentType, superType)) {
                    return true;
                }
            }
            return false;
        }

        if (subType.isParameterizedClass()) {
            ParameterizedClassSymbol subParam = subType.asParameterizedClass();
            ClassSymbol base = subParam.getBase();

            if (isSubtype(base, superType)) {
                return true;
            }

            if (superType.isParameterizedClass()) {
                ParameterizedClassSymbol superParam = superType.asParameterizedClass();
                if (base == superParam.getBase()) {
                    Map<String, Symbol> subArgs = subParam.getTypeArguments();
                    Map<String, Symbol> superArgs = superParam.getTypeArguments();

                    if (subArgs.size() != superArgs.size()) {
                        return false;
                    }

                    for (String key : subArgs.keySet()) {
                        if (!isAssignable(subArgs.get(key), superArgs.get(key))) {
                            return false;
                        }
                    }
                    return true;
                }
            }

            return false;
        }

        if (subType.isTypeParam()) {
            TypeParamSymbol tp = subType.asTypeParam();
            for (BoundSymbol bound : tp.getBounds().values()) {
                Symbol boundType = bound.getType();
                if (boundType != null && isSubtype(boundType, superType)) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    public boolean isComparable(Symbol a, Symbol b) {
        if (a == null || b == null) {
            return false;
        }

        if (isSametype(a, b)) {
            return true;
        }

        if (isNumeric(a) && isNumeric(b)) {
            return true;
        }

        if (isString(a) && isString(b)) {
            return true;
        }

        if (isBool(a) && isBool(b)) {
            return true;
        }

        if (isNull(a) || isNull(b)) {
            return true;
        }

        if (isAssignable(a, b) || isAssignable(b, a)) {
            return true;
        }

        return hasCommonSupertype(a, b);
    }

    public boolean hasCommonSupertype(Symbol a, Symbol b) {
        if (a == null || b == null) {
            return false;
        }

        if (a == b) {
            return true;
        }

        Set<Symbol> supertypesA = getAllSupertypes(a);

        for (Symbol superA : supertypesA) {
            if (isAssignable(b, superA)) {
                return true;
            }
        }

        Set<Symbol> supertypesB = getAllSupertypes(b);

        for (Symbol superB : supertypesB) {
            if (isAssignable(a, superB)) {
                return true;
            }
        }

        for (Symbol superA : supertypesA) {
            for (Symbol superB : supertypesB) {
                if (isAssignable(superA, superB) || isAssignable(superB, superA)) {
                    return true;
                }
            }
        }

        return false;
    }

    public Set<Symbol> getAllSupertypes(Symbol sym) {
        Set<Symbol> result = new LinkedHashSet<>();

        if (sym == null) {
            return result;
        }

        result.add(sym);

        if (sym.isClass()) {
            ClassSymbol cls = sym.asClass();
            for (SuperSymbol parent : cls.getSupers().values()) {
                result.addAll(getAllSupertypes(parent.getType()));
            }
        } else if (sym.isParameterizedClass()) {
            ParameterizedClassSymbol param = sym.asParameterizedClass();
            result.addAll(getAllSupertypes(param.getBase()));
        } else if (sym.isTypeParam()) {
            TypeParamSymbol tp = sym.asTypeParam();
            for (BoundSymbol bound : tp.getBounds().values()) {
                result.addAll(getAllSupertypes(bound.getType()));
            }
        }

        return result;
    }

    public Symbol getLeastUpperBound(Symbol a, Symbol b) {
        if (a == null || b == null) {
            return null;
        }

        if (isAssignable(a, b)) {
            return b;
        }

        if (isAssignable(b, a)) {
            return a;
        }

        Set<Symbol> supertypesA = getAllSupertypes(a);
        Set<Symbol> supertypesB = getAllSupertypes(b);

        List<Symbol> common = new ArrayList<>();
        for (Symbol superA : supertypesA) {
            if (supertypesB.contains(superA)) {
                common.add(superA);
            }
        }

        if (common.isEmpty()) {
            return getAny();
        }

        Symbol result = common.get(0);
        for (Symbol candidate : common) {
            if (isAssignable(candidate, result)) {
                result = candidate;
            }
        }

        return result;
    }

    public boolean isNullable(Symbol sym) {
        if (sym == null) {
            return false;
        }

        if (isNull(sym)) {
            return true;
        }

        return !isPrimitive(sym);
    }

    public boolean isReferenceType(Symbol sym) {
        return sym != null && (sym.isClass() ||
                sym.isParameterizedClass() ||
                sym.isTypeParam());
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
        if (sym == null)
            return false;
        if (sym == getNumber())
            return true;
        if (isInt(sym))
            return true;
        if (isFloat(sym))
            return true;

        return isSubtype(sym, getNumber());
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

    public boolean isPrimitive(Symbol sym) {
        return isBool(sym) || isInt(sym) || isFloat(sym);
    }

    public boolean isNumeric(Symbol sym) {
        return isInt(sym) || isFloat(sym);
    }

    public boolean isIntegral(Symbol sym) {
        return isInt(sym);
    }

    public boolean isFloating(Symbol sym) {
        return isFloat(sym);
    }

    public boolean isClassType(Symbol sym) {
        return sym != null && (sym.isClass() || sym.isParameterizedClass());
    }

    public ClassSymbol getBaseClass(Symbol sym) {
        if (sym == null) {
            return null;
        }
        if (sym.isClass()) {
            return sym.asClass();
        }
        if (sym.isParameterizedClass()) {
            return sym.asParameterizedClass().getBase();
        }
        return null;
    }

    public boolean isSubtypeOf(Symbol sub, Symbol sup) {
        return isSubtype(sub, sup);
    }

    public boolean isSupertypeOf(Symbol sup, Symbol sub) {
        return isSubtype(sub, sup);
    }
}