package com.dev.lang.ast;

import com.dev.lang.util.Position;
import com.dev.lang.util.Positioned;

public abstract class Node implements Positioned {
	protected Position position;

	protected Node() {
		this(null);
	}

	protected Node(Position position) {
		this.position = position;
	}

	@Override
	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}
}