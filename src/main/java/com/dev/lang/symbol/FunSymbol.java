package com.dev.lang.symbol;

import com.dev.lang.ast.BlockStmt;
import java.util.*;

public class FunSymbol extends Symbol {
	private final String name;
	private final List<ParamSymbol> parameters;
	private final Symbol returnType;
	private BlockStmt body;
	private final List<TypeParamSymbol> typeParameters;
	private final ClassSymbol declaringClass;

	public FunSymbol(String name, List<ParamSymbol> parameters, Symbol returnType,
					 ClassSymbol declaringClass,
					 List<TypeParamSymbol> typeParameters) {
		this.name = name;
		this.parameters = parameters != null ? Collections.unmodifiableList(parameters) : Collections.emptyList();
		this.returnType = returnType;
		this.declaringClass = declaringClass;
		this.typeParameters = typeParameters != null ? Collections.unmodifiableList(typeParameters) : Collections.emptyList();
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

	public ClassSymbol getDeclaringClass() {
		return declaringClass;
	}
}