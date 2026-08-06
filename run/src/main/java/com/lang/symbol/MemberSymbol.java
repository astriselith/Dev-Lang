package com.lang.symbol;

public class MemberSymbol extends Symbol {

    protected ClassSymbol declaringClass;

    protected MemberSymbol(String name) {
        super(name);
    }

    public void setDeclaringClass(ClassSymbol declaringClass) {
        this.declaringClass = declaringClass;
    }

    public ClassSymbol getDeclaringClass() {
        return declaringClass;
    }

}
