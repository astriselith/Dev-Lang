package com.lang.ast;

import com.lang.util.Position;
import java.util.ArrayList;
import java.util.List;

public class OperatorDecl extends MemberDecl {
    public Typed type;                
    public int kind;
    public List<ParamDecl> operands = new ArrayList<>();
    public Operator operator;
    public List<TypeParamDecl> typeParameters;
    public BlockStmt body;

    public OperatorDecl() {}

    public OperatorDecl(List<AnnoDecl> annotations, List<Modifier> modifiers,
                        int kind, List<ParamDecl> operands, Operator operator,
                        Typed type, BlockStmt body,
                        List<TypeParamDecl> typeParameters, Position position) {
        super(annotations, modifiers,
              new Identifier(operator.lexeme, position), position);
        this.type = type;
        this.kind = kind;
        this.operands = operands != null ? operands : List.of();
        this.operator = operator;
        this.typeParameters = typeParameters;
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