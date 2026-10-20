package com.lang.symbol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.lang.ast.BlockStmt;

public class FunSymbol extends MemberSymbol {

	private Map<String, TypeParamSymbol> typeParameters;
	private Map<String, ParamSymbol> parameters;
	private boolean pure;

	private BlockStmt body;

	public FunSymbol(String name) {
		super(name);

		this.typeParameters = new LinkedHashMap<>();
		this.parameters = new LinkedHashMap<>();
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

	// Parameters

	public void addParameter(ParamSymbol pSymbol) {
		this.parameters.put(pSymbol.getName(), pSymbol);
	}

	public void setParameters(Map<String, ParamSymbol> parameters) {
		this.parameters.clear();
		if (parameters != null)
			this.parameters.putAll(parameters);
	}

	public ParamSymbol getParameter(String pName) {
		return this.parameters.get(pName);
	}

	public Map<String, ParamSymbol> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}

	public boolean hasParameter(String pName) {
		return this.parameters.containsKey(pName);
	}

	public boolean hasParameters() {
		return !parameters.isEmpty();
	}

	// Body / Pure
	public BlockStmt getBody() {
		return body;
	}

	public void setBody(BlockStmt body) {
		this.body = body;
	}

	public boolean isPure() {
		return this.pure;
	}

	public void setPure(boolean pure) {
		this.pure = pure;
	}
}