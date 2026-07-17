package com.lang.ast;

import com.lang.util.Position;

public class RefTyped extends Typed {

	public final String name;

	public RefTyped(String name, Position position) {
		super(position);
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}
}