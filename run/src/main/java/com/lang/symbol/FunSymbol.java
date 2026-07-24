package com.lang.symbol;

import com.lang.ast.BlockStmt;
import com.lang.ast.Typed;

import java.util.*;

public class FunSymbol extends Symbol {
	private final String name;

	private final List<String> typeParameterTypes;
	private final List<String> parameterTypes;
	private final Typed returnTypeType;

	private Map<String, TypeParamSymbol> typeParameters;
	private Map<String, ParamSymbol> parameters;
	private Symbol returnType;
	private boolean pure;

	private BlockStmt body;

	public FunSymbol(String name, List<String> typeParameterTypes, List<String> parameterTypes, Typed returnTypeType) {
		this.name = name;
		this.typeParameterTypes = typeParameterTypes != null ? typeParameterTypes : new ArrayList<>();
		this.parameterTypes = parameterTypes != null ? parameterTypes : new ArrayList<>();
		this.returnTypeType = returnTypeType;

		this.typeParameters = new LinkedHashMap<>();
		this.parameters = new LinkedHashMap<>();
		this.returnType = null;
	}

	public String getName() {
		return name;
	}

	public List<String> getTypeParameterTypes() {
		return typeParameterTypes;
	}

	public List<String> getParameterTypes() {
		return parameterTypes;
	}

	public Typed getReturnTypeType() {
		return returnTypeType;
	}

	public Map<String, ParamSymbol> getParameters() {
		return parameters;
	}

	public Symbol getReturnType() {
		return returnType;
	}

	public BlockStmt getBody() {
		return body;
	}

	public void setBody(BlockStmt body) {
		this.body = body;
	}

	// TypeParameters

	public void addTypeParameter(TypeParamSymbol tpSymbol) {
		this.typeParameters.put(tpSymbol.getName(), tpSymbol);
	}

	public void setTypeParameters(Map<String, TypeParamSymbol> typeParameters) {
		this.typeParameters.clear();
		if (typeParameters != null)
			this.typeParameters.putAll(typeParameters);
	}

	public TypeParamSymbol getTypeParameter(String tpName) {
		return this.typeParameters.get(tpName);
	}

	public Map<String, TypeParamSymbol> getTypeParameters() {
		return Collections.unmodifiableMap(typeParameters);
	}

	public boolean hasTypeParameter(String tpName) {
		return this.typeParameters.containsKey(tpName);
	}

	public boolean hasTypeParameters() {
		return !typeParameters.isEmpty();
	}

	public boolean isPure() {
		return this.pure;
	}

}