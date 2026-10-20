package com.lang.symbol;

import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolScope {
    private final SymbolScope parent;
    private final Map<String, Symbol> locals;
    private final String name;

    public SymbolScope(String name) {
        this(name, null);
    }

    public SymbolScope(String name, SymbolScope parent) {
        this.name = name;
        this.parent = parent;
        this.locals = new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public SymbolScope getParent() {
        return parent;
    }

    public void putLocal(String name, Symbol symbol) {
        locals.put(name, symbol);
    }

    public Symbol getLocal(String name) {
        return locals.get(name);
    }

    public boolean hasLocal(String name) {
        return locals.containsKey(name);
    }

    public Symbol resolveLocal(String name) {
        Symbol symbol = locals.get(name);
        if (symbol != null) {
            return symbol;
        }
        if (parent != null) {
            return parent.resolveLocal(name);
        }
        return null;
    }

    public Symbol resolve(String name) {
        Symbol symbol = locals.get(name);
        if (symbol != null) {
            return symbol;
        }

        if (parent != null) {
            return parent.resolve(name);
        }

        return null;
    }

    public Map<String, Symbol> getLocals() {
        return new LinkedHashMap<>(locals);
    }

    public SymbolScope child(String name) {
        return new SymbolScope(name, this);
    }

    @Override
    public String toString() {
        return "Scope{" + name +
                ", locals=" + locals.keySet() +
                "}";
    }
}