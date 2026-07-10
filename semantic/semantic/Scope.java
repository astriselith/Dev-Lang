package com.dev.lang.semantic;

import com.dev.lang.symbol.*;
import java.util.*;
import java.util.stream.Collectors;

public class Scope {
	public enum Kind {
		GLOBAL, CLASS, TRAIT, FUN, BLOCK, LOOP
	}

	private final Map<String, Symbol> symbols = new LinkedHashMap<>();
	// Substituição de ESCOPO (compartilhada) - ex: class Box<T> { T value; }
	private final Map<String, Symbol> scopeSubstitution;
	// Substituição de SÍMBOLO (única) - ex: Box<String> box;
	private final Map<String, Map<String, Symbol>> symbolSubstitutions = new HashMap<>();

	private final Scope parent;
	private final String name;
	private final Kind kind;
	private final int depth;

	public Scope(Scope parent, String name, Kind kind) {
		this(parent, name, kind, null);
	}

	public Scope(Scope parent, String name, Kind kind, Map<String, Symbol> substitution) {
		this.parent = parent;
		this.name = name;
		this.kind = kind;
		this.depth = parent != null ? parent.depth + 1 : 0;
		this.scopeSubstitution = substitution != null ? new LinkedHashMap<>(substitution) : Collections.emptyMap();
	}

	public Scope getParent() {
		return parent;
	}
	public String getName() {
		return name;
	}
	public Kind getKind() {
		return kind;
	}
	public int getDepth() {
		return depth;
	}

	// ========== Substituição de ESCOPO ==========
	public Map<String, Symbol> getScopeSubstitution() {
		return Collections.unmodifiableMap(scopeSubstitution);
	}

	public boolean hasScopeSubstitution() {
		return !scopeSubstitution.isEmpty();
	}

	// ========== Substituição de SÍMBOLO ==========

	public void define(Symbol symbol) {
		define(symbol, null);
	}

	public void define(Symbol symbol, Map<String, Symbol> typeSubstitution) {
		if (!symbols.containsKey(symbol.name)) {
			symbols.put(symbol.name, symbol);
			if (typeSubstitution != null && !typeSubstitution.isEmpty()) {
				symbolSubstitutions.put(symbol.name, new LinkedHashMap<>(typeSubstitution));
			}
		}
	}

	public boolean hasSymbolSubstitution(String symbolName) {
		if (symbolSubstitutions.containsKey(symbolName)) {
			return true;
		}
		if (parent != null) {
			return parent.hasSymbolSubstitution(symbolName);
		}
		return false;
	}

	public Map<String, Symbol> getSymbolSubstitution(String symbolName) {
		Map<String, Symbol> sub = symbolSubstitutions.get(symbolName);
		if (sub != null) {
			return Collections.unmodifiableMap(sub);
		}
		if (parent != null) {
			return parent.getSymbolSubstitution(symbolName);
		}
		return null;
	}

	public Map<String, Symbol> getLocalSymbolSubstitution(String symbolName) {
		Map<String, Symbol> sub = symbolSubstitutions.get(symbolName);
		return sub != null ? Collections.unmodifiableMap(sub) : null;
	}

	// ========== Resolução ==========

	public Symbol resolve(String name) {
		// 1. Símbolo local?
		if (symbols.containsKey(name)) {
			return symbols.get(name);
		}

		// 2. Substituição de escopo?
		if (scopeSubstitution.containsKey(name)) {
			return scopeSubstitution.get(name);
		}

		// 3. Busca no pai
		if (parent != null) {
			return parent.resolve(name);
		}

		return null;
	}

	// Resolve o símbolo base (sem considerar substituição de símbolo)
	public Symbol resolveBase(String name) {
		if (symbols.containsKey(name)) {
			return symbols.get(name);
		}
		if (parent != null) {
			return parent.resolveBase(name);
		}
		return null;
	}

	// ========== Métodos auxiliares ==========

	public boolean contains(String name) {
		return symbols.containsKey(name);
	}

	public boolean containsAnywhere(String name) {
		if (symbols.containsKey(name)) return true;
		if (parent != null) return parent.containsAnywhere(name);
		return false;
	}

	public Symbol resolveLocal(String name) {
		return symbols.get(name);
	}

	public Symbol resolveHere(String name) {
		return symbols.get(name);
	}

	public Symbol resolveUpTo(int maxDepth, String name) {
		Symbol symbol = symbols.get(name);
		if (symbol != null) return symbol;
		if (parent != null && maxDepth > 0) {
			return parent.resolveUpTo(maxDepth - 1, name);
		}
		return null;
	}

	public Symbol resolveInDepth(int targetDepth, String name) {
		if (depth == targetDepth) {
			if (symbols.containsKey(name)) return symbols.get(name);
			if (scopeSubstitution.containsKey(name)) return scopeSubstitution.get(name);
			return null;
		}
		if (parent != null) {
			return parent.resolveInDepth(targetDepth, name);
		}
		return null;
	}

	public Symbol resolveInDepthRelative(int relativeDepth, String name) {
		int targetDepth = this.depth - relativeDepth;
		return resolveInDepth(targetDepth, name);
	}

	public Scope createNested(String name, Kind kind) {
		return new Scope(this, name, kind);
	}

	public Scope createNested(String name, Kind kind, Map<String, Symbol> additionalSubstitution) {
		Map<String, Symbol> merged = new LinkedHashMap<>(this.scopeSubstitution);
		if (additionalSubstitution != null) {
			merged.putAll(additionalSubstitution);
		}
		return new Scope(this, name, kind, merged);
	}

	public Map<String, Symbol> getAllSymbols() {
		Map<String, Symbol> all = new LinkedHashMap<>();
		if (parent != null) {
			all.putAll(parent.getAllSymbols());
		}
		all.putAll(symbols);
		return Collections.unmodifiableMap(all);
	}

	public Map<String, Symbol> getLocalSymbols() {
		return Collections.unmodifiableMap(symbols);
	}

	public List<Symbol> getSymbolsByKind(Symbol.Kind kind) {
		return symbols.values().stream()
			   .filter(s -> s.kind == kind)
			   .collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	public <T extends Symbol> List<T> getSymbolsByType(Class<T> type) {
		return symbols.values().stream()
			   .filter(type::isInstance)
			   .map(s -> (T) s)
			   .collect(Collectors.toList());
	}

	public boolean isGlobal() {
		return kind == Kind.GLOBAL;
	}
	public boolean isClass() {
		return kind == Kind.CLASS;
	}
	public boolean isTrait() {
		return kind == Kind.TRAIT;
	}
	public boolean isFun() {
		return kind == Kind.FUN;
	}
	public boolean isBlock() {
		return kind == Kind.BLOCK;
	}
	public boolean isLoop() {
		return kind == Kind.LOOP;
	}

	public void removeSymbol(String name) {
		symbols.remove(name);
		symbolSubstitutions.remove(name);
	}

	public void clear() {
		symbols.clear();
		symbolSubstitutions.clear();
	}

	@Override
	public String toString() {
		return String.format("Scope[%s:%s] depth=%d symbols=%d scopeSub=%s symSubs=%s",
							 name, kind, depth, symbols.size(),
							 scopeSubstitution.keySet(), symbolSubstitutions.keySet());
	}
}