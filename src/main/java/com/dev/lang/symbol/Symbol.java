package com.dev.lang.symbol;

public abstract class Symbol {
	public final int declarationOrder;
	private static int globalCounter = 0;

	protected Symbol() {
		this.declarationOrder = ++globalCounter;
	}

	public boolean isClassOrTrait() {
		return this instanceof ClassOrTraitSymbol;
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

	public boolean isParameterized() {
		return this instanceof ParameterizedClassOrTraitSymbol;
	}

	public ClassOrTraitSymbol asClassOrTrait() {
		return isClassOrTrait() ? (ClassOrTraitSymbol) this : null;
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

	public ParameterizedClassOrTraitSymbol asParameterized() {
		return isParameterized() ? (ParameterizedClassOrTraitSymbol) this : null;
	}

	public static void resetCounter() {
		globalCounter = 0;
	}
}