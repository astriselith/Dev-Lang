package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class SuperDecl extends Decl {
    public Typed type;
    public List<Expr> arguments;

    public SuperDecl() {
    }

    public SuperDecl(Typed type, List<Expr> arguments, Position position) {
        super(position);
        this.type = type;
        this.arguments = arguments;
    }

    @Override
    public <T> T accept(DeclVisitor<T> visitor) {
        return visitor.visitSuperDecl(this);
    }
}