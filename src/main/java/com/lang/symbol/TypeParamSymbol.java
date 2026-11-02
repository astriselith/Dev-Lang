package com.lang.symbol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TypeParamSymbol extends Symbol {

    private final Map<String, BoundSymbol> bounds;

    public TypeParamSymbol(String name) {
        super(name);
        this.bounds = new LinkedHashMap<>();
    }

    public void addBound(BoundSymbol symbol) {
        this.bounds.put(symbol.getName(), symbol);
    }

    public void setBounds(Map<String, BoundSymbol> bounds) {
        this.bounds.clear();
        if (bounds != null) this.bounds.putAll(bounds);
    }

    public boolean hasBound(String className) {
        return this.bounds.containsKey(className);
    }

    public BoundSymbol getBound(String className) {
        return this.bounds.get(className);
    }

    public Map<String, BoundSymbol> getBounds() {
        return Collections.unmodifiableMap(bounds);
    }

    public boolean hasBounds() {
        return !bounds.isEmpty();
    }

    public FunSymbol getFun(String name) {
        Set<ClassSymbol> seen = new LinkedHashSet<>();
        for (BoundSymbol bound : bounds.values()) {
            ClassSymbol cls = toClass(bound.getType());
            while (cls != null && seen.add(cls)) {
                FunSymbol f = cls.getDeclaredFun(name);
                if (f != null) return f;
                cls = cls.getBaseSupers().isEmpty() ? null : cls.getBaseSupers().get(0);
            }
        }
        return null;
    }

    public VarSymbol getVar(String name) {
        Set<ClassSymbol> seen = new LinkedHashSet<>();
        for (BoundSymbol bound : bounds.values()) {
            ClassSymbol cls = toClass(bound.getType());
            while (cls != null && seen.add(cls)) {
                VarSymbol v = cls.getDeclaredVar(name);
                if (v != null) return v;
                cls = cls.getBaseSupers().isEmpty() ? null : cls.getBaseSupers().get(0);
            }
        }
        return null;
    }

    public boolean hasFun(String name) {
        Set<ClassSymbol> seen = new LinkedHashSet<>();
        for (BoundSymbol bound : bounds.values()) {
            ClassSymbol cls = toClass(bound.getType());
            while (cls != null && seen.add(cls)) {
                if (cls.hasDeclaredFun(name)) return true;
                cls = cls.getBaseSupers().isEmpty() ? null : cls.getBaseSupers().get(0);
            }
        }
        return false;
    }

    public boolean hasVar(String name) {
        Set<ClassSymbol> seen = new LinkedHashSet<>();
        for (BoundSymbol bound : bounds.values()) {
            ClassSymbol cls = toClass(bound.getType());
            while (cls != null && seen.add(cls)) {
                if (cls.hasDeclaredVar(name)) return true;
                cls = cls.getBaseSupers().isEmpty() ? null : cls.getBaseSupers().get(0);
            }
        }
        return false;
    }

    private static ClassSymbol toClass(Symbol base) {
        if (base == null) return null;
        if (base.isClass()) return base.asClass();
        if (base.isParameterizedClass()) return base.asParameterizedClass().getBase();
        return null;
    }
}