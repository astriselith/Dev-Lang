package com.lang.ast;

import java.util.ArrayList;
import java.util.List;
import com.lang.util.Position;

public class ClassDecl extends CallableDecl {

    public List<SuperDecl> supers;
    public List<MemberDecl> members;

    public ClassDecl() {
    }

    public ClassDecl(List<AnnoDecl> annotations, List<Modifier> modifiers,
                     Identifier name, List<ParamDecl> parameters,
                     List<SuperDecl> supers, List<TypeParamDecl> typeParameters,
                     List<MemberDecl> members, Position position) {
        super(annotations, modifiers, name,
              selfType(name, typeParameters, position),
              typeParameters, parameters, position);
        this.supers = supers;
        this.members = members;
    }

    private static Typed selfType(Identifier name, List<TypeParamDecl> typeParameters,
            Position position) {
        if (typeParameters == null || typeParameters.isEmpty())
            return new RefTyped(name, position);
        List<Typed> arguments = new ArrayList<>(typeParameters.size());
        for (TypeParamDecl parameter : typeParameters)
            arguments.add(new RefTyped(parameter.name, position));
        return new ParameterizedRefTyped(name, arguments, position);
    }

    @Override
    public <T> T accept(DeclVisitor<T> visitor) {
        return visitor.visitClassDecl(this);
    }
}