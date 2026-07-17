package com.lang.ast;

import com.lang.util.Position;
import com.lang.util.Positioned;

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