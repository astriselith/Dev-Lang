package com.lang.ast;

import com.lang.util.Position;

import java.util.List;

public class OperatorExpr extends Expr {
    public int kind;
    public Expr left;
    public Operator operator;
    public Expr right;
    public List<Typed> typeArguments;

    public OperatorExpr() {
    }

    public OperatorExpr(int kind, Expr left, Operator operator, Expr right,
                        List<Typed> typeArguments, Position position) {
        super(position);
        this.kind = kind;
        this.left = left;
        this.operator = operator;
        this.right = right;
        this.typeArguments = typeArguments;
    }

    public boolean isPrefix()  { return kind == Operator.PREFIX;  }
    public boolean isInfix()   { return kind == Operator.INFIX;   }
    public boolean isPostfix() { return kind == Operator.POSTFIX; }
    public boolean isUnary()   { return isPrefix() || isPostfix(); }
    public boolean isBinary()  { return isInfix(); }
}