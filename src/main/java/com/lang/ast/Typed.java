package com.lang.ast;

import com.lang.util.Position;

public abstract class Typed extends Node {

	public Typed() {
	}

	public Typed(Position position) {
		super(position);
	}

	public boolean isRef() {
		return this instanceof RefTyped;
	}

	public boolean isParameterizedRef() {
		return this instanceof ParameterizedRefTyped;
	}

	public RefTyped asRef() {
		return (RefTyped) this;
	}

	public ParameterizedRefTyped asParameterizedRef() {
		return (ParameterizedRefTyped) this;
	}

	public abstract String getName();

	public abstract String getSignature();

}