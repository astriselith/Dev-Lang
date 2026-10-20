// ./src/main/java/com/lang/Main.java
package com.lang;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.lang.analyzer.Analyzer;
import com.lang.lexer.Lexer;
import com.lang.linker.Linker;
import com.lang.ls.LangServer;
import com.lang.parser.Parser;
import com.lang.register.Register;
import com.lang.source.Source;
import com.lang.symbol.SymbolTable;
import com.lang.unit.CompilationUnit;
import com.lang.util.Diagnostic;

public class Main {

	public static void main(String[] args) throws Exception {
		if (isLspMode(args)) {
			runLsp(args);
			return;
		}
		runInteractive();
	}

	private static boolean isLspMode(String[] args) {
		if (args == null) return false;
		for (String a : args) {
			if ("--ls".equals(a) || "--language-server".equals(a)) {
				return true;
			}
		}
		return false;
	}

	private static void runLsp(String[] args) throws Exception {
		List<String> forwarded = new ArrayList<>();
		for (String a : args) {
			if ("--ls".equals(a) || "--language-server".equals(a)) continue;
			forwarded.add(a);
		}

		System.err.println("[LANG] iniciando servidor LSP via stdio...");
		LangServer.main(forwarded.toArray(new String[0]));
	}

	// ==================== MODO INTERATIVO ====================

	private static void runInteractive() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			try {
				System.out.print("> ");
				String line = reader.readLine();
				if (line == null) break;

				line = line.trim();
				if (line.isEmpty()) continue;

				if (line.equals("exit") || line.equals("quit")) break;

				if (line.equals("help") || line.equals("?")) {
					printHelp();
					continue;
				}

				if (line.startsWith("file ")) {
					String filesPart = line.substring(5).trim();
					if (filesPart.isEmpty()) {
						System.err.println("Especifique pelo menos um arquivo");
						continue;
					}

					String[] fileNames = filesPart.split("\\s+");
					List<CompilationUnit> units = new ArrayList<>();

					for (String fileName : fileNames) {
						Path path = Paths.get(fileName);
						if (!Files.exists(path)) {
							System.err.println("Arquivo não encontrado: " + path);
							continue;
						}
						units.add(new CompilationUnit(
								path.toString(), path.getFileName().toString()));
					}

					if (!units.isEmpty()) processFiles(units);
					continue;
				}

				System.err.println("Comando inválido. Use 'help'");
			} catch (Exception e) {
				System.err.println("Erro: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private static void printHelp() {
		System.out.println();
		System.out.println("  file <arquivo1.lang> [arquivo2.lang ...] - Processa arquivos");
		System.out.println("  help - Mostra esta ajuda");
		System.out.println("  exit - Sai do programa");
		System.out.println();
		System.out.println("Uso via CLI:");
		System.out.println("  java -jar lang.jar                          # modo interativo");
		System.out.println("  java -jar lang.jar --ls                     # servidor LSP em stdin/stdout");
		System.out.println("  java -jar lang.jar --language-server        # idem");
		System.out.println();
	}

	private static void processFiles(List<CompilationUnit> units) {
		SymbolTable symbolTable = new SymbolTable();

		List<CompilationUnit> parsed = new ArrayList<>();
		for (CompilationUnit unit : units) {
			if (parse(unit)) parsed.add(unit);
		}

		for (CompilationUnit unit : parsed) {
			new Register(symbolTable, unit).register();
		}
		for (CompilationUnit unit : parsed) {
			new Linker(symbolTable, unit).link();
		}
		for (CompilationUnit unit : parsed) {
			new Analyzer(symbolTable, unit).analyze();
		}

		for (CompilationUnit unit : units) {
			unit.printReport(Diagnostic.HINT);
		}
	}

	private static boolean parse(CompilationUnit unit) {
		try {
			Path path = Paths.get(unit.getFilePath());
			Source source = Source.fromFile(path, unit);
			Lexer lexer = new Lexer(source, unit);
			Parser parser = new Parser(lexer, unit);
			parser.parse();
		} catch (Exception e) {
			unit.addDiagnostic("PARSER", Diagnostic.error(null,
					"Erro ao processar " + unit.getFileName() + ": " + e.getMessage()));
		}
		return !hasErrors(unit, "PARSER");
	}

	private static boolean hasErrors(CompilationUnit unit, String tag) {
		return unit.getDiagnostics(tag).stream().anyMatch(Diagnostic::isError);
	}
}