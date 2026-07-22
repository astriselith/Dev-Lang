package com.lang.ast;

import com.lang.util.Position;

public abstract class Typed extends Node {

	public Typed() {
	}

	public Typed(Position position) {
		super(position);
	}

	public abstract String getName();

	public boolean isRef() {
		return this instanceof RefTyped;
	}

	public boolean isParameterizedRef() {
		return this instanceof ParameterizedRefTyped;
	}

	public <T> T accept(TypedVisitor<T> visitor) {
		if (this instanceof RefTyped)
			visitor.visitRefTyped((RefTyped) this);
		if (this instanceof ParameterizedRefTyped)
			visitor.visitParameterizedRefTyped((ParameterizedRefTyped) this);

		throw new UnsupportedOperationException("Unknown typed type: " + this.getClass().getName());
	}
}