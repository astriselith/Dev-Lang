package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class AnnoDecl extends Decl {
    public Typed type;
    public List<Expr> arguments;

    public AnnoDecl() {
    }

    public AnnoDecl(Typed type, List<Expr> arguments, Position position) {
        super(position);
        this.type = type;
        this.arguments = arguments;
    }

    @Override
    public <T> T accept(DeclVisitor<T> visitor) {
        return visitor.visitAnnoDecl(this);
    }
}
