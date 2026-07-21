package com.lang.ast;

import com.lang.util.Position;

public class RefTyped extends Typed {

	public String name;

	public RefTyped() {
	}

	public RefTyped(String name, Position position) {
		super(position);
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
}