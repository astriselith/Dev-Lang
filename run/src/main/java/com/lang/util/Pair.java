package com.lang.util;

import java.util.Objects;

public final class Pair<A, B> {
	public final A first;
	public final B second;

	private Pair(A first, B second) {
		this.first = first;
		this.second = second;
	}

	public static <A, B> Pair<A, B> of(A first, B second) {
		return new Pair<>(first, second);
	}

	public A getFirst() {
		return first;
	}

	public B getSecond() {
		return second;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Pair<?, ?>))
			return false;

		Pair<?, ?> pair = (Pair<?, ?>) o;

		return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
	}

	@Override
	public int hashCode() {
		return Objects.hash(first, second);
	}

	@Override
	public String toString() {
		return "Pair[" + first + ", " + second + "]";
	}
}