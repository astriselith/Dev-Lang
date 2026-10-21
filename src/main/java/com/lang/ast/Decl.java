package com.lang.ast;

import com.lang.util.Position;

public class Decl extends Node {

    public Decl() {
    }

    public Decl(Position position) {
        super(position);
    }

    public <T> T accept(DeclVisitor<T> visitor) {
    if (this instanceof ClassDecl)     return visitor.visitClassDecl((ClassDecl) this);
    if (this instanceof TypeParamDecl) return visitor.visitTypeParamDecl((TypeParamDecl) this);
    if (this instanceof SuperDecl)     return visitor.visitSuperDecl((SuperDecl) this);
    if (this instanceof BoundDecl)     return visitor.visitBoundDecl((BoundDecl) this);
    if (this instanceof VarDecl)       return visitor.visitVarDecl((VarDecl) this);
    if (this instanceof FunDecl)       return visitor.visitFunDecl((FunDecl) this);
    if (this instanceof ParamDecl)     return visitor.visitParamDecl((ParamDecl) this);
    throw new IllegalArgumentException("Unknown declaration type");
}
}
