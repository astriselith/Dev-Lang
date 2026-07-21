package com.lang.ast;

import com.lang.util.Position;

public class ParenthesisExpr extends Expr {
    public Expr inner;

    public ParenthesisExpr() {
    }

    public ParenthesisExpr(Expr inner, Position position) {
        super(position);
        this.inner = inner;
    }

}