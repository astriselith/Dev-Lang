package com.lang.symbol;

public class BoundSymbol extends Symbol {
    private Symbol type;

    public BoundSymbol(String name) {
        super(name);
    }

    public void setType(Symbol type) { this.type = type; }
    public Symbol getType() { return type; }
}