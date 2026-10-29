package com.lang.ast;

import java.util.ArrayList;
import java.util.List;

import com.lang.util.Position;

public class BoundDecl extends Decl {
    public Identifier name;
    public List<TypeParamDecl> typeParameters = new ArrayList<>();

    public BoundDecl() {}

    public BoundDecl(Identifier name, List<TypeParamDecl> typeParameters, Position position) {
        super(position);
        this.name = name;
        this.typeParameters = typeParameters;
    }

    @Override
    public <T> T accept(DeclVisitor<T> visitor) {
        return visitor.visitBoundDecl(this);
    }
}