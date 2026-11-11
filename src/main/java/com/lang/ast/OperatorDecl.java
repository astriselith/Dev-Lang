package com.lang.ast;

import java.util.List;
import com.lang.util.Position;

public class OperatorDecl extends CallableDecl {

    public int kind;
    public Operator operator;
    public BlockStmt body;

    public OperatorDecl() {
    }

    public OperatorDecl(List<AnnoDecl> annotations, List<Modifier> modifiers,
                        int kind, List<ParamDecl> parameters, Operator operator,
                        Typed type, BlockStmt body,
                        List<TypeParamDecl> typeParameters, Position position) {
        super(annotations, modifiers,
              new Identifier(operator.lexeme, position),
              type, typeParameters, parameters, position);
        this.kind = kind;
        this.operator = operator;
        this.body = body;
    }

    public boolean isPrefix()  { return kind == Operator.PREFIX; }
    public boolean isInfix()   { return kind == Operator.INFIX; }
    public boolean isPostfix() { return kind == Operator.POSTFIX; }

    @Override
    public <T> T accept(DeclVisitor<T> visitor) {
        return visitor.visitOperatorDecl(this);
    }
}