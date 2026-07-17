package com.lang.ast;

import com.lang.util.Position;
import java.util.ArrayList;
import java.util.List;

public class FunDeclStmt extends Stmt {
	public final String name;
	public final List<TypeParamDeclStmt> typeParameters;
	public final List<ParamDeclStmt> parameters;
	public final Typed returnType;
	public final BlockStmt body;

	public FunDeclStmt(String name,
					   List<TypeParamDeclStmt> typeParameters,
					   List<ParamDeclStmt> parameters,
					   Typed returnType, BlockStmt body, Position position) {
		super(position);
		this.name = name;
		this.typeParameters = typeParameters != null ? typeParameters : new ArrayList<>();
		this.parameters = parameters != null ? parameters : new ArrayList<>();
		this.returnType = returnType;
		this.body = body;
	}

	public boolean hasTypeParameters() {
		return !typeParameters.isEmpty();
	}

	public boolean hasReturnType() {
		return returnType != null;
	}

	public boolean hasBody() {
		return body != null;
	}

	public List<String> getParameterNames() {
		List<String> names = new ArrayList<>();
		for (ParamDeclStmt param : parameters) {
			names.add(param.name);
		}
		return names;
	}

	public List<Typed> getParameterTypes() {
		List<Typed> types = new ArrayList<>();
		for (ParamDeclStmt param : parameters) {
			types.add(param.type);
		}
		return types;
	}

	public List<String> getTypeParameterNames() {
		List<String> names = new ArrayList<>();
		for (TypeParamDeclStmt tp : typeParameters) {
			names.add(tp.name);
		}
		return names;
	}

	public int getArity() {
		return parameters.size();
	}

	public int getTypeArity() {
		return typeParameters.size();
	}
}