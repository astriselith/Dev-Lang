package com.dev.lang.semantic;

import com.dev.lang.ast.*;
import com.dev.lang.symbol.*;
import com.dev.lang.unit.CompilationUnit;

public class Semantic {
	private final SymbolTable symbolTable;
	private final CompilationUnit compilationUnit;
	private final ClassBinder binder;
	private final ClassResolver resolver;

	public Semantic(SymbolTable symbolTable, CompilationUnit compilationUnit) {
		this.symbolTable = symbolTable;
		this.compilationUnit = compilationUnit;
		this.binder = new ClassBinder(symbolTable, compilationUnit);
		this.resolver = new ClassResolver(symbolTable, compilationUnit);
	}

	public void analyze(CompilationUnit unit) {
		binder.bindAll();
		resolver.resolveAll();
	}

	public boolean hasErrors() {
		return compilationUnit.hasErrors();
	}

	public boolean hasWarnings() {
		return compilationUnit.hasWarnings();
	}

	public void printReport() {
		compilationUnit.printReport();
	}
}