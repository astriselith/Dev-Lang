package com.lang.symbol;

import com.lang.ast.BlockStmt;
import java.util.*;

public class FunSymbol extends Symbol {
	private final String name;
	private final List<ParamSymbol> parameters;
	private final Symbol returnType;
	private BlockStmt body;
	private final List<TypeParamSymbol> typeParameters;

	public FunSymbol(String name, List<ParamSymbol> parameters, Symbol returnType,
			List<TypeParamSymbol> typeParameters) {
		this.name = name;
		this.parameters = parameters != null ? Collections.unmodifiableList(parameters) : Collections.emptyList();
		this.returnType = returnType;
		this.typeParameters = typeParameters != null ? Collections.unmodifiableList(typeParameters)
				: Collections.emptyList();
	}

	public String getName() {
		return name;
	}

	public List<ParamSymbol> getParameters() {
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

	public int getArity() {
		return parameters.size();
	}

	public boolean isGeneric() {
		return !typeParameters.isEmpty();
	}

	public List<TypeParamSymbol> getTypeParameters() {
		return typeParameters;
	}
}