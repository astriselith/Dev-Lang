package com.lang.unit;

import com.lang.ast.ClassDecl;
import com.lang.util.Position;
import com.lang.util.Positioned;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompilationUnit {
	private final String filePath;
	private final String fileName;

	public final List<ClassDecl> classes;

	private final List<CompilationException> errors;
	private final List<WarningException> warnings;

	public CompilationUnit(String filePath, String fileName) {
		this.filePath = filePath;
		this.fileName = fileName;

		this.classes = new ArrayList<>();
		this.errors = new ArrayList<>();
		this.warnings = new ArrayList<>();
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

	public String getFilePath() {
		return filePath;
	}

	public String getFileName() {
		return fileName;
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

	public void addClass(ClassDecl classDecl) {
		classes.add(classDecl);
	}

	public void addClasses(List<ClassDecl> classes) {
		this.classes.addAll(classes);
	}

	public List<ClassDecl> getClasses() {
		return Collections.unmodifiableList(classes);
	}

	public boolean hasClass(String name) {
		for (ClassDecl cls : classes) {
			if (cls.name.sourceEquals(name)) {
				return true;
			}
		}
		return false;
	}

	public ClassDecl getClass(String name) {
		for (ClassDecl cls : classes) {
			if (cls.name.sourceEquals(name)) {
				return cls;
			}
		}
		return null;
	}

	public int getClassCount() {
		return classes.size();
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

}