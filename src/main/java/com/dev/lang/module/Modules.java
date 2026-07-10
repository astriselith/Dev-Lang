package com.dev.lang.module;

import com.dev.lang.lexer.Lexer;
import com.dev.lang.parser.Parser;
import com.dev.lang.source.SourceStream;
import com.dev.lang.unit.CompilationUnit;
import com.dev.lang.util.Positioned;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.*;

public final class Modules {

	private static final String TAG = "MODULE";
	private static final String MODULE_EXTENSION = ".module";
	private static final String LANG_EXTENSION = ".lang";
	private static final String MODULE_JSON = "module.json";

	private final List<Module> modules;
	private final Map<String, Map<String, Map<String, Module>>> index;

	public Modules() {
		this.modules = new ArrayList<>();
		this.index = new LinkedHashMap<>();
	}

	public Modules(List<Module> modules) {
		this.modules = new ArrayList<>(modules);
		this.index = new LinkedHashMap<>();
		indexModules();
	}

	public Module load(Path path) {
		Module module = loadModule(path);
		add(module);
		return module;
	}

	public Module load(String path) {
		Module module = loadModule(Path.of(path));
		add(module);
		return module;
	}

	public Modules loadAll(Path... paths) {
		for (Path path : paths) {
			Module module = loadModule(path);
			add(module);
		}
		resolve();
		return this;
	}

	public Modules loadAll(String... paths) {
		for (String path : paths) {
			Module module = loadModule(Path.of(path));
			add(module);
		}
		resolve();
		return this;
	}

	public Modules loadAll(List<Path> paths) {
		for (Path path : paths) {
			Module module = loadModule(path);
			add(module);
		}
		resolve();
		return this;
	}

	private Module loadModule(Path modulePath) {
		Objects.requireNonNull(modulePath, "Module path cannot be null");
		CompilationUnit unit = new CompilationUnit();

		if (!Files.exists(modulePath)) {
			unit.addError(unit.error(TAG, "Module not found: " + modulePath, (Positioned) null));
			return Module.empty(unit, modulePath);
		}

		if (!modulePath.toString().endsWith(MODULE_EXTENSION)) {
			unit.addError(unit.error(TAG, "Not a module file: " + modulePath, (Positioned) null));
			return Module.empty(unit, modulePath);
		}

		Module module = null;

		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(modulePath))) {
			ZipEntry entry;

			while ((entry = zis.getNextEntry()) != null) {
				String name = entry.getName();

				if (entry.isDirectory()) {
					zis.closeEntry();
					continue;
				}

				if (name.endsWith(MODULE_JSON)) {
					// Use ModuleParser to parse the JSON
					SourceStream source = new SourceStream(zis, entry.getSize());
					Lexer lexer = new Lexer(source, unit);
					ModuleParser parser = new ModuleParser(lexer, unit);
					module = parser.parse();
					zis.closeEntry();
					continue;
				}

				if (name.endsWith(LANG_EXTENSION)) {
					String fileName = name.contains("/") ? name.substring(name.lastIndexOf("/") + 1) : name;
					System.out.println("  Loading: " + fileName + " (" + entry.getSize() + " bytes)");

					SourceStream source = new SourceStream(zis, entry.getSize());
					Lexer lexer = new Lexer(source, unit);
					Parser parser = new Parser(lexer, unit);
					parser.parse();

					if (unit.hasErrors()) {
						System.err.println("    Errors found");
					} else {
						System.out.println("    OK");
					}
					zis.closeEntry();
				}
			}

			if (module == null) {
				unit.addWarning(unit.warn(TAG, "No module-info.json found in module: " + modulePath.getFileName(), (Positioned) null));
				module = Module.empty(unit, modulePath);
			}

		} catch (IOException e) {
			unit.addError(unit.error(TAG, "Error loading module: " + e.getMessage(), (Positioned) null, e));
			module = Module.empty(unit, modulePath);
		}

		return module;
	}

	public Modules resolve() {
		indexModules();
		linkModules();
		return this;
	}

	private void indexModules() {
		index.clear();
		for (Module module : modules) {
			addToIndex(module);
		}
	}

	private void addToIndex(Module module) {
		String packageName = module.getPackageName();
		String artifactId = module.getArtifactId();
		String version = module.getVersion();

		Map<String, Map<String, Module>> artifactMap = index.get(packageName);
		if (artifactMap == null) {
			artifactMap = new LinkedHashMap<>();
			index.put(packageName, artifactMap);
		}

		Map<String, Module> versionMap = artifactMap.get(artifactId);
		if (versionMap == null) {
			versionMap = new LinkedHashMap<>();
			artifactMap.put(artifactId, versionMap);
		}

		versionMap.put(version, module);
	}

	private Module findByIndex(String packageName, String artifactId, String version) {
		Map<String, Map<String, Module>> artifactMap = index.get(packageName);
		if (artifactMap == null) {
			return null;
		}

		Map<String, Module> versionMap = artifactMap.get(artifactId);
		if (versionMap == null) {
			return null;
		}

		return versionMap.get(version);
	}

	private Module findByIndex(String packageName, String artifactId) {
		Map<String, Map<String, Module>> artifactMap = index.get(packageName);
		if (artifactMap == null) {
			return null;
		}

		Map<String, Module> versionMap = artifactMap.get(artifactId);
		if (versionMap == null) {
			return null;
		}

		for (Map.Entry<String, Module> entry : versionMap.entrySet()) {
			return entry.getValue();
		}
		return null;
	}

	private void removeFromIndex(Module module) {
		String packageName = module.getPackageName();
		String artifactId = module.getArtifactId();
		String version = module.getVersion();

		Map<String, Map<String, Module>> artifactMap = index.get(packageName);
		if (artifactMap != null) {
			Map<String, Module> versionMap = artifactMap.get(artifactId);
			if (versionMap != null) {
				versionMap.remove(version);
				if (versionMap.isEmpty()) {
					artifactMap.remove(artifactId);
				}
			}
			if (artifactMap.isEmpty()) {
				index.remove(packageName);
			}
		}
	}

	private void linkModules() {
		for (Module module : modules) {
			List<Module> linkedDeps = new ArrayList<>();

			for (Module dep : module.getDependencies()) {
				Module resolved = findByIndex(
									  dep.getPackageName(),
									  dep.getArtifactId(),
									  dep.getVersion()
								  );

				if (resolved == null) {
					resolved = findByIndex(
								   dep.getPackageName(),
								   dep.getArtifactId()
							   );
				}

				if (resolved != null) {
					linkedDeps.add(resolved);
				} else {
					linkedDeps.add(dep);
				}
			}

			module.setDependencies(linkedDeps);
		}
	}

	public List<Module> getAll() {
		return modules;
	}

	public Optional<Module> find(String packageName, String artifactId) {
		return Optional.ofNullable(findByIndex(packageName, artifactId));
	}

	public Optional<Module> find(String packageName, String artifactId, String version) {
		return Optional.ofNullable(findByIndex(packageName, artifactId, version));
	}

	public List<Module> findAll(String packageName, String artifactId) {
		List<Module> result = new ArrayList<>();
		Map<String, Map<String, Module>> artifactMap = index.get(packageName);
		if (artifactMap != null) {
			Map<String, Module> versionMap = artifactMap.get(artifactId);
			if (versionMap != null) {
				result.addAll(versionMap.values());
			}
		}
		return result;
	}

	public List<Module> findAll(String packageName) {
		List<Module> result = new ArrayList<>();
		Map<String, Map<String, Module>> artifactMap = index.get(packageName);
		if (artifactMap != null) {
			for (Map<String, Module> versionMap : artifactMap.values()) {
				result.addAll(versionMap.values());
			}
		}
		return result;
	}

	public List<Module> findDependents(String packageName, String artifactId) {
		List<Module> dependents = new ArrayList<>();
		for (Module module : modules) {
			if (module.hasDependency(packageName, artifactId)) {
				dependents.add(module);
			}
		}
		return dependents;
	}

	public List<Module> findDependents(String packageName, String artifactId, String version) {
		List<Module> dependents = new ArrayList<>();
		for (Module module : modules) {
			if (module.hasDependency(packageName, artifactId, version)) {
				dependents.add(module);
			}
		}
		return dependents;
	}

	public List<Module> getValid() {
		List<Module> valid = new ArrayList<>();
		for (Module module : modules) {
			if (module.isValid()) {
				valid.add(module);
			}
		}
		return valid;
	}

	public List<Module> getInvalid() {
		List<Module> invalid = new ArrayList<>();
		for (Module module : modules) {
			if (!module.isValid()) {
				invalid.add(module);
			}
		}
		return invalid;
	}

	public Modules add(Module module) {
		if (module != null && !module.isEmpty()) {
			modules.add(module);
			addToIndex(module);
		}
		return this;
	}

	public Modules addAll(Collection<Module> modules) {
		for (Module module : modules) {
			add(module);
		}
		return this;
	}

	public Modules remove(Module module) {
		modules.remove(module);
		removeFromIndex(module);
		return this;
	}

	public Modules clear() {
		modules.clear();
		index.clear();
		return this;
	}

	public int size() {
		return modules.size();
	}

	public boolean isEmpty() {
		return modules.isEmpty();
	}

	public boolean hasMissingDependencies() {
		for (Module module : modules) {
			for (Module dep : module.getDependencies()) {
				Module resolved = findByIndex(
									  dep.getPackageName(),
									  dep.getArtifactId(),
									  dep.getVersion()
								  );
				if (resolved == null) {
					resolved = findByIndex(
								   dep.getPackageName(),
								   dep.getArtifactId()
							   );
				}
				if (resolved == null) {
					return true;
				}
			}
		}
		return false;
	}

	public List<Module> getMissingDependencies() {
		List<Module> missing = new ArrayList<>();
		for (Module module : modules) {
			for (Module dep : module.getDependencies()) {
				Module resolved = findByIndex(
									  dep.getPackageName(),
									  dep.getArtifactId(),
									  dep.getVersion()
								  );
				if (resolved == null) {
					resolved = findByIndex(
								   dep.getPackageName(),
								   dep.getArtifactId()
							   );
				}
				if (resolved == null) {
					missing.add(dep);
				}
			}
		}
		return missing;
	}

	@Override
	public String toString() {
		return "Modules[" + size() + "]";
	}
}