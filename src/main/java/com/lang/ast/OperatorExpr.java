package com.lang.ast;

import com.lang.util.Position;

import java.util.ArrayList;
import java.util.List;

public class OperatorExpr extends Expr {
    public int kind;
    public List<Expr> operands = new ArrayList<>();
    public Operator operator;
    public List<Typed> typeArguments;

    public OperatorExpr() {
    }

    public OperatorExpr(int kind, List<Expr> operands, Operator operator,
                        List<Typed> typeArguments, Position position) {
        super(position);
        this.kind = kind;
        this.operands = operands != null ? operands : List.of();
        this.operator = operator;
        this.typeArguments = typeArguments;
    }

    public boolean isPrefix()  { return kind == Operator.PREFIX;  }
    public boolean isInfix()   { return kind == Operator.INFIX;   }
    public boolean isPostfix() { return kind == Operator.POSTFIX; }
    public boolean isUnary()   { return isPrefix() || isPostfix(); }
    public boolean isBinary()  { return isInfix(); }
}