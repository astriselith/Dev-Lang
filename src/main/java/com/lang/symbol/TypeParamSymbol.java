package com.lang.symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypeParamSymbol extends Symbol {

    private final Map<String, BoundSymbol> bounds;

    public TypeParamSymbol(String name) {
        super(name);
        this.bounds = new LinkedHashMap<>();
    }

    // Bounds

    public void addBound(BoundSymbol symbol) {
        this.bounds.put(symbol.getName(), symbol);
    }

    public void setBounds(Map<String, BoundSymbol> bounds) {
        this.bounds.clear();
        if (bounds != null)
            this.bounds.putAll(bounds);
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

    // Bounds (util)

    public List<ClassSymbol> getBaseBounds() {
        List<ClassSymbol> result = new ArrayList<>();
        for (BoundSymbol bound : bounds.values()) {
            Symbol base = bound.getType();
            if (base == null)
                continue;
            if (base.isClass()) {
                result.add(base.asClass());
            } else if (base.isParameterizedClass()) {
                result.add(base.asParameterizedClass().getBase());
            }
        }
        return Collections.unmodifiableList(result);
    }

    public Symbol getMember(String name) {
        for (ClassSymbol bound : getBaseBounds()) {
            Symbol result = bound.getMember(name);
            if (result != null)
                return result;
        }

        return null;
    }
}