package com.lang.module;

import com.lang.unit.CompilationUnit;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Module {

	private static final String UNKNOWN = "unknown";
	private static final String DEFAULT_VERSION = "0.0.0";

	private final String packageName;
	private final String artifactId;
	private final String version;
	private final List<Module> dependencies;
	private final CompilationUnit unit;
	private final Path path;

	Module(String packageName, String artifactId, String version, List<Module> dependencies, CompilationUnit unit, Path path) {
		this.packageName = packageName != null && !packageName.isBlank() ? packageName : UNKNOWN;
		this.artifactId = artifactId != null && !artifactId.isBlank() ? artifactId : UNKNOWN;
		this.version = version != null && !version.isBlank() ? version : DEFAULT_VERSION;
		this.dependencies = new ArrayList<>();
		if (dependencies != null) {
			this.dependencies.addAll(dependencies);
		}
		this.unit = unit != null ? unit : new CompilationUnit();
		this.path = path;
	}

	public static Module empty(CompilationUnit unit) {
		return new Module(UNKNOWN, UNKNOWN, DEFAULT_VERSION, List.of(), unit, null);
	}

	public static Module empty(CompilationUnit unit, Path path) {
		return new Module(UNKNOWN, UNKNOWN, DEFAULT_VERSION, List.of(), unit, path);
	}

	public static Module empty() {
		return new Module(UNKNOWN, UNKNOWN, DEFAULT_VERSION, List.of(), new CompilationUnit(), null);
	}

	public static Module from(String packageName, String artifactId, String version) {
		return new Module(packageName, artifactId, version, List.of(), new CompilationUnit(), null);
	}

	public String getPackageName() {
		return packageName;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public List<Module> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<Module> dependencies) {
		this.dependencies.clear();
		if (dependencies != null) {
			this.dependencies.addAll(dependencies);
		}
	}


	public CompilationUnit getCompilationUnit() {
		return unit;
	}

	public Optional<Path> getPath() {
		return Optional.ofNullable(path);
	}

	public boolean hasErrors() {
		return unit.hasErrors();
	}

	public boolean hasWarnings() {
		return unit.hasWarnings();
	}

	public boolean isValid() {
		return unit.isSuccessful();
	}

	public boolean isEmpty() {
		return UNKNOWN.equals(packageName) || UNKNOWN.equals(artifactId);
	}

	public boolean hasDependency(String packageName, String artifactId) {
		for (Module dep : dependencies) {
			if (dep.getPackageName().equals(packageName) && dep.getArtifactId().equals(artifactId)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasDependency(String packageName, String artifactId, String version) {
		for (Module dep : dependencies) {
			if (dep.getPackageName().equals(packageName) &&
					dep.getArtifactId().equals(artifactId) &&
					dep.getVersion().equals(version)) {
				return true;
			}
		}
		return false;
	}

	public Optional<Module> findDependency(String packageName, String artifactId) {
		for (Module dep : dependencies) {
			if (dep.getPackageName().equals(packageName) && dep.getArtifactId().equals(artifactId)) {
				return Optional.of(dep);
			}
		}
		return Optional.empty();
	}

	public Optional<Module> findDependency(String packageName, String artifactId, String version) {
		for (Module dep : dependencies) {
			if (dep.getPackageName().equals(packageName) &&
					dep.getArtifactId().equals(artifactId) &&
					dep.getVersion().equals(version)) {
				return Optional.of(dep);
			}
		}
		return Optional.empty();
	}

	public String toDependencyString() {
		return packageName + ":" + artifactId + ":" + version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Module module = (Module) o;
		return Objects.equals(packageName, module.packageName) &&
			   Objects.equals(artifactId, module.artifactId) &&
			   Objects.equals(version, module.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(packageName, artifactId, version);
	}

	@Override
	public String toString() {
		return "Module{" +
			   "packageName='" + packageName + '\'' +
			   ", artifactId='" + artifactId + '\'' +
			   ", version='" + version + '\'' +
			   ", dependencies=" + dependencies.size() +
			   '}';
	}
}