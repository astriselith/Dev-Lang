package com.lang.symbol;

public abstract class Symbol {

	private final String name;

	public final int declarationOrder;
	private static int globalCounter = 0;

	protected Symbol(String name) {
		this.name = name;
		this.declarationOrder = ++globalCounter;
	}

	public String getName() {
		return name;
	}

	public boolean isClass() {
		return this instanceof ClassSymbol;
	}

	public boolean isMember() {
		return this instanceof MemberSymbol;
	}

	public boolean isSuper() {
		return this instanceof SuperSymbol;
	}

	public boolean isFun() {
		return this instanceof FunSymbol;
	}

	public boolean isParam() {
		return this instanceof ParamSymbol;
	}

	public boolean isVar() {
		return this instanceof VarSymbol;
	}

	public boolean isTypeParam() {
		return this instanceof TypeParamSymbol;
	}

	public boolean isParameterizedClass() {
		return this instanceof ParameterizedClassSymbol;
	}

	public ClassSymbol asClass() {
		return isClass() ? (ClassSymbol) this : null;
	}

	public MemberSymbol asMember() {
		return isMember() ? (MemberSymbol) this : null;
	}

	public SuperSymbol asSuper() {
		return isSuper() ? (SuperSymbol) this : null;
	}

	public FunSymbol asFun() {
		return isFun() ? (FunSymbol) this : null;
	}

	public ParamSymbol asParam() {
		return isParam() ? (ParamSymbol) this : null;
	}

	public VarSymbol asVar() {
		return isVar() ? (VarSymbol) this : null;
	}

	public TypeParamSymbol asTypeParam() {
		return isTypeParam() ? (TypeParamSymbol) this : null;
	}

	public ParameterizedClassSymbol asParameterizedClass() {
		return isParameterizedClass() ? (ParameterizedClassSymbol) this : null;
	}

	public static void resetCounter() {
		globalCounter = 0;
	}

	@Override
	public String toString() {
		return name;
	}
}