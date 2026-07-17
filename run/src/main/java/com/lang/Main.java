package com.lang;

import com.lang.lexer.Lexer;
import com.lang.parser.Parser;
import com.lang.source.SourceFile;
import com.lang.symbol.SymbolTable;
import com.lang.tools.Printer;
import com.lang.unit.CompilationUnit;
import com.lang.unit.CompilationException;
import com.lang.unit.WarningException;
import com.lang.module.Module;
import com.lang.module.Modules;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class Main {
	private static boolean verbose = true;
	private static CompilationUnit sharedUnit = new CompilationUnit();
	private static String currentDir = System.getProperty("user.dir");
	private static final Modules modules = new Modules();

	public static void main(String[] args) {
		currentDir += "/tests";
		try {
			if (args.length == 0) {
				runInteractive();
			} else {
				for (String arg : args) {
					if (arg.equals("-v")) {
						verbose = true;
					} else {
						processFile(arg);
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Erro: " + e.getMessage());
			if (verbose) e.printStackTrace();
			System.exit(1);
		}
	}

	private static void runInteractive() throws Exception {
		System.out.println("Lang CLI - Testes de Módulos");
		System.out.println("Comandos: parse, module, modules, resolve, status, reset, verbose, exit");
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.print("> ");
			String line = reader.readLine();
			if (line == null) break;

			line = line.trim();
			if (line.isEmpty()) continue;

			if (line.equals("exit") || line.equals("quit")) {
				if (!modules.isEmpty()) {
					System.out.println("\n--- Módulos Carregados ---");
					for (Module module : modules.getAll()) {
						System.out.println("  " + module.toDependencyString());
						if (module.hasErrors()) {
							System.out.println("    ⚠ Erros");
						}
					}
				}
				break;
			}

			if (line.equals("help")) {
				System.out.println("Comandos:");
				System.out.println("  parse <file>              - Parseia arquivo .lang");
				System.out.println("  module <path>             - Carrega módulo .module");
				System.out.println("  modules                   - Lista módulos");
				System.out.println("  resolve                   - Resolve dependências");
				System.out.println("  status                    - Mostra status");
				System.out.println("  reset                     - Reseta tudo");
				System.out.println("  verbose                   - Verbose mode");
				System.out.println("  exit                      - Sai");
				continue;
			}

			if (line.equals("verbose")) {
				verbose = !verbose;
				System.out.println("verbose = " + verbose);
				continue;
			}

			if (line.equals("status")) {
				System.out.println("Classes: " + sharedUnit.getClassCount());
				System.out.println("Módulos: " + modules.size());
				System.out.println("Erros: " + sharedUnit.getErrorCount());
				System.out.println("Dependências faltantes: " + (modules.hasMissingDependencies() ? "Sim" : "Não"));
				continue;
			}

			if (line.equals("reset")) {
				sharedUnit = new CompilationUnit();
				new SymbolTable();
				modules.clear();
				System.out.println("Resetado");
				continue;
			}

			if (line.equals("modules")) {
				handleModules();
				continue;
			}

			if (line.equals("resolve")) {
				handleResolve();
				continue;
			}

			if (line.startsWith("parse ")) {
				String file = line.substring(6).trim();
				processFile(file);
				continue;
			}

			if (line.startsWith("module ")) {
				String path = line.substring(7).trim();
				handleModuleAdd(path);
				continue;
			}

			System.out.println("Comando inválido. Use 'help'");
		}
	}

	private static void handleModules() {
		if (modules.isEmpty()) {
			System.out.println("Nenhum módulo carregado.");
			return;
		}

		System.out.println("\n=== Módulos (" + modules.size() + ") ===");
		for (int i = 0; i < modules.getAll().size(); i++) {
			Module m = modules.getAll().get(i);
			System.out.printf("%d. %s %s%n", i + 1, m.toDependencyString(), m.isValid() ? "✓" : "✗");

			if (!m.getDependencies().isEmpty()) {
				String deps = m.getDependencies().stream()
							  .map(Module::toDependencyString)
							  .collect(Collectors.joining(", "));
				System.out.println("   Deps: " + deps);
			}
		}

		if (modules.hasMissingDependencies()) {
			System.out.println("\n⚠ Dependências faltantes:");
			for (Module dep : modules.getMissingDependencies()) {
				System.out.println("  " + dep.toDependencyString());
			}
		}
	}

	private static void handleResolve() {
		if (modules.isEmpty()) {
			System.out.println("Nenhum módulo.");
			return;
		}

		modules.resolve();
		System.out.println("✓ Resolvido!");

		if (modules.hasMissingDependencies()) {
			System.out.println("⚠ Dependências faltantes:");
			for (Module dep : modules.getMissingDependencies()) {
				System.out.println("  " + dep.toDependencyString());
			}
		}
	}

	private static void handleModuleAdd(String modulePath) {
		try {
			Path path = Paths.get(currentDir).resolve(modulePath).normalize();

			if (!Files.exists(path)) {
				System.err.println("Módulo não encontrado: " + path);
				return;
			}

			if (!path.toString().endsWith(".module")) {
				System.err.println("Arquivoe ter extensão .module");
				return;
			}

			if (verbose) System.out.println("Carregando: " + path);

			Module module = modules.load(path);

			if (module.hasErrors()) {
				System.out.println("✗ Falha ao carregar módulo");
				module.getCompilationUnit().printReport();
				return;
			}

			CompilationUnit unit = module.getCompilationUnit();
			if (unit != null) {
				unit.forEachClass(classDecl -> sharedUnit.addClass(classDecl));
				for (CompilationException e : unit.getErrors()) {
					sharedUnit.addError(e);
				}
				for (WarningException w : unit.getWarnings()) {
					sharedUnit.addWarning(w);
				}
			}

			System.out.println("✓ " + module.toDependencyString() + " carregado!");

		} catch (Exception e) {
			System.err.println("Erro: " + e.getMessage());
			if (verbose) e.printStackTrace();
		}
	}

	private static void processFile(String path) {
		try {
			Path input = Paths.get(currentDir).resolve(path).normalize();

			if (!Files.exists(input)) {
				System.err.println("Arquivo não encontrado: " + input);
				return;
			}

			if (!input.toString().endsWith(".lang")) {
				System.err.println("Arquivoe ter extensão .lang");
				return;
			}

			if (verbose) System.out.println("Parsing: " + input);

			SourceFile source = new SourceFile(input);
			Lexer lexer = new Lexer(source, sharedUnit);
			Parser parser = new Parser(lexer, sharedUnit);
			parser.parse();

			if (sharedUnit.hasErrors()) {
				System.out.println("✗ " + input.getFileName() + " - Falhou");
				if (verbose) {
					for (CompilationException e : sharedUnit.getErrors()) {
						System.err.println("  " + e.getMessage());
					}
				}
				return;
			}

			System.out.println("✓ " + input.getFileName() + " - OK (" + sharedUnit.getClassCount() + " classes)");

			if (verbose) {
				Printer printer = new Printer();
				sharedUnit.forEachClass(classDecl -> {
					System.out.println(printer.print(classDecl));
				});
			}

		} catch (Exception e) {
			System.err.println("Erro ao processar: " + e.getMessage());
			if (verbose) e.printStackTrace();
		}
	}
}