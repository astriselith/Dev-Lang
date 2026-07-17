package com.lang.ast;

import com.lang.util.Position;

public abstract class Typed extends Node {
	protected Typed(Position position) {
		super(position);
	}

	public abstract String getName();

	public boolean isRef() {
		return this instanceof RefTyped;
	}

	public boolean isParameterizedRef() {
		return this instanceof ParameterizedRefTyped;
	}
}