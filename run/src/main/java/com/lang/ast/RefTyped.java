package com.lang.ast;

import com.lang.util.Position;

public class RefTyped extends Typed {

	public Identifier name;

	public RefTyped() {
	}

	public RefTyped(Identifier name, Position position) {
		super(position);
		this.name = name;
	}

	@Override
	public String getName() {
		return name.source;
	}
}