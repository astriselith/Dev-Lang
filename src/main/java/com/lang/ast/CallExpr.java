package com.lang.ast;

import java.util.List;

import com.lang.util.Position;

public class CallExpr extends Expr {
	public Expr callee;
	public List<Typed> typeArguments;
	public List<Expr> arguments;

	public CallExpr() {
	}

	public CallExpr(Expr callee, List<Typed> typeArguments, List<Expr> arguments, Position position) {
		super(position);
		this.callee = callee;
		this.typeArguments = typeArguments;
		this.arguments = arguments;
	}
}