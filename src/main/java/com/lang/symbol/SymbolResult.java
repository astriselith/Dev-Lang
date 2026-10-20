package com.lang.symbol;

import java.util.LinkedHashMap;
import java.util.Map;

public class SymbolResult {
    private final Symbol raw;
    private final Symbol type;
    private final Map<String, Symbol> substituitions;

    public SymbolResult(Symbol raw, Symbol type, Map<String, Symbol> substituitions) {
        this.raw = raw;
        this.type = type;
        this.substituitions = substituitions;
    }

    public Symbol getRaw() {
        return raw;
    }

    public Symbol getType() {
        return type;
    }

    public Map<String, Symbol> getSubstituitions() {
        return substituitions;
    }

    public static SymbolResult of(SymbolResult other) {
        return new SymbolResult(other.raw, other.type, other.substituitions);
    }

    public static SymbolResult of(Symbol raw, Symbol type, Map<String, Symbol> substituitions) {
        return new SymbolResult(raw, type, substituitions);
    }

    public static SymbolResult of(Symbol raw, Symbol type) {
        return new SymbolResult(raw, type, new LinkedHashMap<>());
    }
}
