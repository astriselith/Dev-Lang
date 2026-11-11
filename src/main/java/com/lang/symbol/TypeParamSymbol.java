package com.lang.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeParamSymbol extends Symbol {

    private final Map<String, Symbol> bounds;

    public TypeParamSymbol(String name) {
        super(name);
        this.bounds = new LinkedHashMap<>();
    }

    public void addBound(Symbol symbol) {
        this.bounds.put(symbol.getName(), symbol);
    }

    public void setBounds(Map<String, Symbol> bounds) {
        this.bounds.clear();
        if (bounds != null)
            this.bounds.putAll(bounds);
    }

    public boolean hasBound(String className) {
        return this.bounds.containsKey(className);
    }

    public Symbol getBound(String className) {
        return this.bounds.get(className);
    }

    public Map<String, Symbol> getBounds() {
        return Collections.unmodifiableMap(bounds);
    }

    public boolean hasBounds() {
        return !bounds.isEmpty();
    }

    public List<ClassSymbol> getBaseBounds() {
        List<ClassSymbol> result = new ArrayList<>();
        for (Symbol bound : bounds.values()) {
            if (bound == null)
                continue;
            if (bound.isClass()) {
                result.add(bound.asClass());
            } else if (bound.isParameterizedClass()) {
                result.add(bound.asParameterizedClass().getBase());
            }
        }
        return Collections.unmodifiableList(result);
    }

    public FunSymbol getFun(String name) {
        Set<ClassSymbol> seen = new LinkedHashSet<>();
        for (ClassSymbol cls : getBaseBounds()) {
            while (cls != null && seen.add(cls)) {
                FunSymbol f = cls.getDeclaredFun(name);
                if (f != null)
                    return f;
                cls = cls.getBaseSupers().isEmpty() ? null : cls.getBaseSupers().get(0);
            }
        }
        return null;
    }

    public VarSymbol getVar(String name) {
        Set<ClassSymbol> seen = new LinkedHashSet<>();
        for (ClassSymbol cls : getBaseBounds()) {
            while (cls != null && seen.add(cls)) {
                VarSymbol v = cls.getDeclaredVar(name);
                if (v != null)
                    return v;
                cls = cls.getBaseSupers().isEmpty() ? null : cls.getBaseSupers().get(0);
            }
        }
        return null;
    }

    public boolean hasFun(String name) {
        Set<ClassSymbol> seen = new LinkedHashSet<>();
        for (ClassSymbol cls : getBaseBounds()) {
            while (cls != null && seen.add(cls)) {
                if (cls.hasDeclaredFun(name))
                    return true;
                cls = cls.getBaseSupers().isEmpty() ? null : cls.getBaseSupers().get(0);
            }
        }
        return false;
    }

    public boolean hasVar(String name) {
        Set<ClassSymbol> seen = new LinkedHashSet<>();
        for (ClassSymbol cls : getBaseBounds()) {
            while (cls != null && seen.add(cls)) {
                if (cls.hasDeclaredVar(name))
                    return true;
                cls = cls.getBaseSupers().isEmpty() ? null : cls.getBaseSupers().get(0);
            }
        }
        return false;
    }
}