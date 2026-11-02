package com.lang.symbol;

public abstract class MemberSymbol extends Symbol {

    private ClassSymbol declaringClass;
    private Symbol type;

    protected MemberSymbol(String name) {
        super(name);
    }

    public void setDeclaringClass(ClassSymbol declaringClass) {
        this.declaringClass = declaringClass;
    }

    public ClassSymbol getDeclaringClass() {
        return declaringClass;
    }

    public void setType(Symbol type) {
        this.type = type;
    }

    public Symbol getType() {
        return type;
    }
}
