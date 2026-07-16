package com.dev.lang.unit;

import com.dev.lang.ast.ClassDeclStmt;
import com.dev.lang.util.Position;
import com.dev.lang.util.Positioned;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompilationUnit {
	private final List<ClassDeclStmt> classes;
	private final List<CompilationException> errors;
	private final List<WarningException> warnings;

	public CompilationUnit() {
		this.classes = new ArrayList<>();
		this.errors = new ArrayList<>();
		this.warnings = new ArrayList<>();
	}

	public CompilationUnit(CompilationUnit other) {
		this.classes = new ArrayList<>(other.classes);
		this.errors = new ArrayList<>(other.errors);
		this.warnings = new ArrayList<>(other.warnings);
	}

	public CompilationException error(String tag, String message, Positioned positioned) {
		return new CompilationException(tag, message, positioned);
	}

	public CompilationException error(String tag, String message, Position position) {
		return new CompilationException(tag, message, position);
	}

	public CompilationException error(String tag, String message, Positioned positioned, Throwable cause) {
		return new CompilationException(tag, message, positioned, cause);
	}

	public CompilationException error(String tag, String message, Position position, Throwable cause) {
		return new CompilationException(tag, message, position, cause);
	}

	public void addError(CompilationException error) {
		errors.add(error);
	}

	public void addErrors(List<CompilationException> errors) {
		this.errors.addAll(errors);
	}

	public WarningException warn(String tag, String message, Positioned positioned) {
		return new WarningException(tag, message, positioned);
	}

	public WarningException warn(String tag, String message, Position position) {
		return new WarningException(tag, message, position);
	}

	public void addWarning(WarningException warning) {
		warnings.add(warning);
	}

	public void addWarnings(List<WarningException> warnings) {
		this.warnings.addAll(warnings);
	}

	public List<CompilationException> getErrors() {
		return Collections.unmodifiableList(errors);
	}

	public List<WarningException> getWarnings() {
		return Collections.unmodifiableList(warnings);
	}

	public List<CompilationException> getErrors(String tag) {
		List<CompilationException> result = new ArrayList<>();
		for (CompilationException e : errors) {
			if (e.getTag().equals(tag)) {
				result.add(e);
			}
		}
		return result;
	}

	public List<WarningException> getWarnings(String tag) {
		List<WarningException> result = new ArrayList<>();
		for (WarningException w : warnings) {
			if (w.getTag().equals(tag)) {
				result.add(w);
			}
		}
		return result;
	}

	public boolean hasErrors() {
		return !errors.isEmpty();
	}

	public boolean hasErrors(String tag) {
		return !getErrors(tag).isEmpty();
	}

	public boolean hasWarnings() {
		return !warnings.isEmpty();
	}

	public boolean hasWarnings(String tag) {
		return !getWarnings(tag).isEmpty();
	}

	public int getErrorCount() {
		return errors.size();
	}

	public int getWarningCount() {
		return warnings.size();
	}

	public void addClass(ClassDeclStmt classDecl) {
		classes.add(classDecl);
	}

	public void addClasses(List<ClassDeclStmt> classes) {
		this.classes.addAll(classes);
	}

	public List<ClassDeclStmt> getClasses() {
		return Collections.unmodifiableList(classes);
	}

	public boolean hasClass(String name) {
		for (ClassDeclStmt cls : classes) {
			if (cls.name.equals(name)) {
				return true;
			}
		}
		return false;
	}

	public ClassDeclStmt getClass(String name) {
		for (ClassDeclStmt cls : classes) {
			if (cls.name.equals(name)) {
				return cls;
			}
		}
		return null;
	}

	public int getClassCount() {
		return classes.size();
	}

	public void forEachClass(java.util.function.Consumer<ClassDeclStmt> action) {
		classes.forEach(action);
	}

	public void printReport() {
		System.out.println("\n=== COMPILATION REPORT ===");

		if (hasErrors()) {
			System.out.println("\nERRORS (" + getErrorCount() + "):");
			for (CompilationException e : errors) {
				System.out.printf("  [%s] %s%n", e.getTag(), e.getMessage());
			}
		}

		if (hasWarnings()) {
			System.out.println("\nWARNINGS (" + getWarningCount() + "):");
			for (WarningException w : warnings) {
				System.out.printf("  [%s] %s%n", w.getTag(), w.getMessage());
			}
		}

		if (!hasErrors() && !hasWarnings()) {
			System.out.println("\nNo errors or warnings found!");
		} else if (!hasErrors()) {
			System.out.println("\nCompilation successful with warnings!");
		} else {
			System.out.println("\nCompilation failed with errors!");
		}
	}

	public boolean isSuccessful() {
		return !hasErrors();
	}

	public static CompilationUnit of(CompilationUnit unit1, CompilationUnit unit2) {
		CompilationUnit result = new CompilationUnit();

		result.addClasses(unit1.classes);
		result.addClasses(unit2.classes);
		result.addErrors(unit1.errors);
		result.addErrors(unit2.errors);
		result.addWarnings(unit1.warnings);
		result.addWarnings(unit2.warnings);

		return result;
	}
}