package com.lang.ast;

import com.lang.util.Position;
import java.util.List;

public class CallExpr extends Expr {
	public final Expr callee;
	public final List<Typed> typeArguments;
	public final List<Expr> arguments;

	public CallExpr(Expr callee, List<Typed> typeArguments, List<Expr> arguments, Position position) {
		super(position);
		this.callee = callee;
		this.typeArguments = typeArguments != null ? typeArguments : List.of();
		this.arguments = arguments != null ? arguments : List.of();
	}

	public boolean isGeneric() {
		return !typeArguments.isEmpty();
	}

	public boolean hasArguments() {
		return !arguments.isEmpty();
	}

	public int getArity() {
		return arguments.size();
	}

	public int getTypeArity() {
		return typeArguments.size();
	}
}