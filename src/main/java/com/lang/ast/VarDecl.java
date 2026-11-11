package com.lang.ast;

import java.util.List;
import com.lang.util.Position;

public class VarDecl extends MemberDecl {
    public Typed type;           
    public Expr value;

    public VarDecl() {}

    public VarDecl(List<AnnoDecl> annotations, List<Modifier> modifiers,
                   Identifier name, Typed type, Expr value, Position position) {
        super(annotations, modifiers, name, position);
        this.type = type;
        this.value = value;
    }

    @Override
    public <T> T accept(DeclVisitor<T> visitor) {
        return visitor.visitVarDecl(this);
    }
}