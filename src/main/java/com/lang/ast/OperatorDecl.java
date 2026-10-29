package com.lang.ast;

import com.lang.util.Position;

import java.util.List;

public class OperatorDecl extends MemberDecl {
    public int kind;
    public ParamDecl left;
    public Operator operator;
    public ParamDecl right;
    public List<TypeParamDecl> typeParameters;
    public BlockStmt body;

    public OperatorDecl() {
    }

    public OperatorDecl(int kind, ParamDecl left, Operator operator, ParamDecl right,
                        Typed type, BlockStmt body,
                        List<TypeParamDecl> typeParameters, Position position) {
        super(new Identifier(operator.lexeme, position), type, position);
        this.kind = kind;
        this.left = left;
        this.operator = operator;
        this.right = right;
        this.typeParameters = typeParameters;
        this.body = body;
    }

    public boolean isPrefix()  { return kind == Operator.PREFIX;  }
    public boolean isInfix()   { return kind == Operator.INFIX;   }
    public boolean isPostfix() { return kind == Operator.POSTFIX; }
    public boolean isUnary()   { return isPrefix() || isPostfix(); }
    public boolean isBinary()  { return isInfix(); }

    @Override
    public <T> T accept(DeclVisitor<T> visitor) {
        return visitor.visitOperatorDecl(this);
    }
}