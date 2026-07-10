package com.dev.lang.symbol;

import com.dev.lang.ast.BlockStmt;
import com.dev.lang.ast.Modifier;
import java.util.*;

public class FunSymbol extends Symbol {
	private final Modifier modifier;
	private final String name;
	private final List<ParamSymbol> parameters;
	private final Symbol returnType;
	private BlockStmt body;
	private final List<TypeParamSymbol> typeParameters;
	private final ClassOrTraitSymbol declaringClassOrTrait;

	public FunSymbol(Modifier modifier, String name, List<ParamSymbol> parameters, Symbol returnType,
					 ClassOrTraitSymbol declaringClassOrTrait,
					 List<TypeParamSymbol> typeParameters) {
		this.modifier = modifier;
		this.name = name;
		this.parameters = parameters != null ? Collections.unmodifiableList(parameters) : Collections.emptyList();
		this.returnType = returnType;
		this.declaringClassOrTrait = declaringClassOrTrait;
		this.typeParameters = typeParameters != null ? Collections.unmodifiableList(typeParameters) : Collections.emptyList();
	}

	public Modifier getModifier() {
		return modifier;
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

	public ClassOrTraitSymbol getDeclaringClassOrTrait() {
		return declaringClassOrTrait;
	}
}