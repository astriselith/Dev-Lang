package com.lang.ast;

import com.lang.util.Position;

public abstract class Expr extends Node {
	protected Expr() {
		super();
	}

	protected Expr(Position position) {
		super(position);
	}

	public <T> T accept(ExprVisitor<T> visitor) {
		if (this instanceof LiteralExpr) return visitor.visitLiteralExpr((LiteralExpr) this);
		if (this instanceof RefExpr) return visitor.visitRefExpr((RefExpr) this);
		if (this instanceof AssignExpr) return visitor.visitAssignExpr((AssignExpr) this);
		if (this instanceof BinaryExpr) return visitor.visitBinaryExpr((BinaryExpr) this);
		if (this instanceof UnaryExpr) return visitor.visitUnaryExpr((UnaryExpr) this);
		if (this instanceof TernaryExpr) return visitor.visitTernaryExpr((TernaryExpr) this);
		if (this instanceof CallExpr) return visitor.visitCallExpr((CallExpr) this);
		if (this instanceof IndexAccessExpr) return visitor.visitIndexAccessExpr((IndexAccessExpr) this);
		if (this instanceof MemberAccessExpr) return visitor.visitMemberAccessExpr((MemberAccessExpr) this);

		throw new UnsupportedOperationException("Unknown expression type: " + this.getClass().getName());
	}
}