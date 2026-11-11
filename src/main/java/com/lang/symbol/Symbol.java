package com.lang.symbol;

import com.lang.ast.Node;

public abstract class Symbol {

    public Node node;

    private final String name;

    public final int declarationOrder;
    private static int globalCounter = 0;

    protected Symbol(String name) {
        this.name = name;
        this.declarationOrder = ++globalCounter;
    }

    public String getName() {
        return name;
    }

    public boolean isClass()              { return this instanceof ClassSymbol; }
    public boolean isSuper()              { return this instanceof SuperSymbol; }
    public boolean isAnno()               { return this instanceof AnnoSymbol; }
    public boolean isMember()             { return this instanceof MemberSymbol; }
    public boolean isCallable()           { return this instanceof CallableSymbol; }
    public boolean isFun()                { return this instanceof FunSymbol; }
    public boolean isOperator()           { return this instanceof OperatorSymbol; }
    public boolean isParam()              { return this instanceof ParamSymbol; }
    public boolean isVar()                { return this instanceof VarSymbol; }
    public boolean isTypeParam()          { return this instanceof TypeParamSymbol; }
    public boolean isParameterizedClass() { return this instanceof ParameterizedClassSymbol; }

    public ClassSymbol asClass()                          { return isClass() ? (ClassSymbol) this : null; }
    public SuperSymbol asSuper()                          { return isSuper() ? (SuperSymbol) this : null; }
    public AnnoSymbol asAnno()                            { return isAnno() ? (AnnoSymbol) this : null; }
    public MemberSymbol asMember()                        { return isMember() ? (MemberSymbol) this : null; }
    public CallableSymbol asCallable()                    { return isCallable() ? (CallableSymbol) this : null; }
    public FunSymbol asFun()                              { return isFun() ? (FunSymbol) this : null; }
    public OperatorSymbol asOperator()                    { return isOperator() ? (OperatorSymbol) this : null; }
    public ParamSymbol asParam()                          { return isParam() ? (ParamSymbol) this : null; }
    public VarSymbol asVar()                              { return isVar() ? (VarSymbol) this : null; }
    public TypeParamSymbol asTypeParam()                  { return isTypeParam() ? (TypeParamSymbol) this : null; }
    public ParameterizedClassSymbol asParameterizedClass(){ return isParameterizedClass() ? (ParameterizedClassSymbol) this : null; }

    public static void resetCounter() {
        globalCounter = 0;
    }

    @Override
    public String toString() {
        return name;
    }
}