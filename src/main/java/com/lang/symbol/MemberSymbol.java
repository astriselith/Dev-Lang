package com.lang.symbol;

public class MemberSymbol extends Symbol {

    protected ClassSymbol declaringClass;
    protected Symbol type;

    protected MemberSymbol(String name) {
        super(name);
    }

    public void setType(Symbol type) {
        this.type = type;
    }

    public Symbol getType() {
        return type;
    }

    public void setDeclaringClass(ClassSymbol declaringClass) {
        this.declaringClass = declaringClass;
    }

    public ClassSymbol getDeclaringClass() {
        return declaringClass;
    }
}