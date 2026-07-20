package com.lang.ast;

import com.lang.util.Position;

public class ParenthesisExpr extends Expr {
    public final Expr inner;

    public ParenthesisExpr(Expr inner, Position position) {
        this.inner = inner;
    }
}