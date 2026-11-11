package com.lang.ast;

import java.util.List;
import com.lang.util.Position;

public class ClassDecl extends MemberDecl {
    public List<ParamDecl> parameters;
    public List<SuperDecl> supers;
    public List<TypeParamDecl> typeParameters;
    public List<MemberDecl> members;

    public ClassDecl() {}

    public ClassDecl(List<AnnoDecl> annotations, List<Modifier> modifiers,
                     Identifier name, List<ParamDecl> parameters,
                     List<SuperDecl> supers, List<TypeParamDecl> typeParameters,
                     List<MemberDecl> members, Position position) {
        super(annotations, modifiers, name, position);
        this.parameters = parameters;
        this.supers = supers;
        this.typeParameters = typeParameters;
        this.members = members;
    }

    @Override
    public <T> T accept(DeclVisitor<T> visitor) {
        return visitor.visitClassDecl(this);
    }
}