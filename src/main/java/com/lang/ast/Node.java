package com.lang.ast;

import com.lang.symbol.Symbol;
import com.lang.util.Position;
import com.lang.util.Positioned;

public abstract class Node implements Positioned {
	public Position position;

	public Symbol symbol;

	public Node() {
	}

	public Node(Position position) {
		this.position = position;
	}

	@Override
	public Position getPosition() {
		return position;
	}
}