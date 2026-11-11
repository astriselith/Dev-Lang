package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public abstract class CallableDecl extends MemberDecl {

    public Typed type;
    public List<TypeParamDecl> typeParameters;
    public List<ParamDecl> parameters;

    protected CallableDecl() {
    }

    protected CallableDecl(List<AnnoDecl> annotations, List<Modifier> modifiers,
            Identifier name, Typed type,
            List<TypeParamDecl> typeParameters,
            List<ParamDecl> parameters,
            Position position) {
        super(annotations, modifiers, name, position);
        this.type = type;
        this.typeParameters = typeParameters;
        this.parameters = parameters;
    }

    @Override
    public abstract <T> T accept(DeclVisitor<T> visitor);
}