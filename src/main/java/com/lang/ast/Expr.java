package com.lang.ast;

import com.lang.util.Position;

public abstract class Expr extends Node {

    public Expr() {
    }

    public Expr(Position position) {
        super(position);
    }

    public <T> T accept(ExprVisitor<T> visitor) {
    if (this instanceof LiteralExpr)      return visitor.visitLiteralExpr((LiteralExpr) this);
    if (this instanceof RefExpr)          return visitor.visitRefExpr((RefExpr) this);
    if (this instanceof MemberExpr)       return visitor.visitMemberExpr((MemberExpr) this);
    if (this instanceof CallExpr)         return visitor.visitCallExpr((CallExpr) this);
    if (this instanceof ParenthesisExpr)  return visitor.visitParenthesisExpr((ParenthesisExpr) this);
    if (this instanceof OperatorExpr)     return visitor.visitOperatorExpr((OperatorExpr) this);
    throw new UnsupportedOperationException("Unknown expression type: " + getClass().getName());
}
}