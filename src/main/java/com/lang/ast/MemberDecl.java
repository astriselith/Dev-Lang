package com.lang.ast;

import java.util.List;
import com.lang.util.Position;

public abstract class MemberDecl extends Decl {
    public List<AnnoDecl> annotations;
    public List<Modifier> modifiers;
    public Identifier name;

    public MemberDecl() {}

    public MemberDecl(List<AnnoDecl> annotations, List<Modifier> modifiers,
                      Identifier name, Position position) {
        super(position);
        this.annotations = annotations;
        this.modifiers = modifiers;
        this.name = name;
    }

    @Override
    public abstract <T> T accept(DeclVisitor<T> visitor);
}