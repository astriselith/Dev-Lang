package com.dev.lang.semantic;

import com.dev.lang.ast.Typed;
import com.dev.lang.symbol.Symbol;
import java.util.*;

/**
 * Resultado de uma resolução de expressão.
 * Apenas carrega: tipo + substituição + símbolo.
 */
public class Result {
	private final Typed type;
	private final Map<String, Symbol> substitution;
	private final Symbol symbol;

	public Result(Typed type, Map<String, Symbol> substitution, Symbol symbol) {
		this.type = type;
		this.substitution = substitution != null
							? Collections.unmodifiableMap(new LinkedHashMap<>(substitution))
							: null;
		this.symbol = symbol;
	}

	public Result(Typed type, Map<String, Symbol> substitution) {
		this(type, substitution, null);
	}

	public Result(Typed type) {
		this(type, null, null);
	}

	public Result(Symbol symbol) {
		this(null, null, symbol);
	}

	public Typed getType() {
		return type;
	}

	public Map<String, Symbol> getSubstitution() {
		return substitution;
	}

	public Symbol getSymbol() {
		return symbol;
	}

	public boolean hasType() {
		return type != null;
	}

	public boolean hasSubstitution() {
		return substitution != null && !substitution.isEmpty();
	}

	public boolean hasSymbol() {
		return symbol != null;
	}

	@Override
	public String toString() {
		return String.format("Result{type=%s, sub=%s, sym=%s}",
							 type != null ? type.getName() : "null",
							 substitution != null ? substitution.keySet() : "null",
							 symbol != null ? symbol.name : "null");
	}
}