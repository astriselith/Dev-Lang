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
import com.lang.parser.Parser;
import com.lang.register.Register;
import com.lang.source.SourceFile;
import com.lang.symbol.SymbolTable;
import com.lang.unit.CompilationException;
import com.lang.unit.CompilationUnit;

public class Main {
	private static boolean verbose = true;

	public static void main(String[] args) {
		runInteractive();
	}

	private static void runInteractive() {
		System.out.println("Lang CLI - Analisador de Classes");
		System.out.println("Comandos:");
		System.out.println("  file: <arquivo1.lang> [arquivo2.lang ...] - Processa arquivos");
		System.out.println("  verbose - Alterna modo verbose");
		System.out.println("  help - Mostra esta ajuda");
		System.out.println("  exit - Sai do programa");
		System.out.println();

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			try {
				System.out.print("> ");
				String line = reader.readLine();
				if (line == null) {
					break;
				}

				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}

				if (line.equals("exit") || line.equals("quit")) {
					System.out.println("Saindo...");
					break;
				}

				if (line.equals("help") || line.equals("?")) {
					printHelp();
					continue;
				}

				if (line.equals("verbose")) {
					verbose = !verbose;
					System.out.println("verbose = " + verbose);
					continue;
				}

				if (line.startsWith("file:")) {
					String filesPart = line.substring(5).trim();
					if (filesPart.isEmpty()) {
						System.out.println("Erro: especifique pelo menos um arquivo");
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
						if (!path.toString().endsWith(".lang")) {
							System.err.println("Arquivo deve ter extensão .lang: " + path);
							continue;
						}
						units.add(new CompilationUnit(path.toString(), path.getFileName().toString()));
					}

					if (!units.isEmpty()) {
						processFiles(units);
					}
					continue;
				}

				System.out.println("Comando inválido. Use 'help'");
			} catch (Exception e) {
				System.err.println("Erro: " + e.getMessage());
				if (verbose) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void printHelp() {
		System.out.println();
		System.out.println("Comandos disponíveis:");
		System.out.println("  file: <arquivo1.lang> [arquivo2.lang ...] - Processa arquivos");
		System.out.println("  verbose - Alterna modo verbose");
		System.out.println("  help - Mostra esta ajuda");
		System.out.println("  exit - Sai do programa");
		System.out.println();
	}

	private static void processFiles(List<CompilationUnit> units) {
		System.out.println("\nProcessando " + units.size() + " arquivo(s)...");
		List<CompilationUnit> validUnits = new ArrayList<>();

		System.out.println("\n--- Fase 1: Parsing ---");
		for (CompilationUnit unit : units) {
			if (verbose) {
				System.out.println("  Parsing: " + unit.getFileName());
			}

			try {
				Path path = Paths.get(unit.getFilePath());
				SourceFile source = new SourceFile(path);
				Lexer lexer = new Lexer(source, unit);
				Parser parser = new Parser(lexer, unit);
				parser.parse();

			} catch (Exception e) {
				unit.addError(new CompilationException(
						"PARSER", "Erro ao processar " + unit.getFileName() + ": " + e.getMessage(),
						(com.lang.util.Position) null));
				if (verbose) {
					e.printStackTrace();
				}
			}

			if (unit.hasErrors("PARSER")) {
				System.out.println("  ✗ " + unit.getFileName() + " - Falhou");
				for (CompilationException e : unit.getErrors("PARSER")) {
					System.err.println("      " + e.getMessage());
				}
			} else {
				System.out.println("  ✓ " + unit.getFileName() + " - OK (" + unit.getClassCount() + " classes)");
				validUnits.add(unit);
			}
		}

		if (validUnits.isEmpty()) {
			System.out.println("\nNenhum arquivo válido para análise.");
			return;
		}

		SymbolTable symbolTable = new SymbolTable();

		System.out.println("\n--- Fase 2: Registro ---");
		for (CompilationUnit unit : validUnits) {
			if (verbose) {
				System.out.println("  Registrando: " + unit.getFileName());
			}

			Register register = new Register(symbolTable, unit);
			register.register();

			if (unit.hasErrors("REGISTER")) {
				System.out.println("  ✗ " + unit.getFileName() + " - Falhou no registro");
				for (CompilationException e : unit.getErrors("REGISTER")) {
					System.err.println("      " + e.getMessage());
				}
			} else {
				System.out.println("  ✓ " + unit.getFileName() + " - Registro OK");
			}
		}

		System.out.println("\n--- Fase 3: Linking ---");
		for (CompilationUnit unit : validUnits) {
			if (unit.hasErrors("REGISTER")) {
				continue;
			}

			if (verbose) {
				System.out.println("  Linkando: " + unit.getFileName());
			}

			Linker linker = new Linker(symbolTable, unit);
			linker.link();

			if (unit.hasErrors("LINKER")) {
				System.out.println("  ✗ " + unit.getFileName() + " - Falhou no linking");
				for (CompilationException e : unit.getErrors("LINKER")) {
					System.err.println("      " + e.getMessage());
				}
			} else {
				System.out.println("  ✓ " + unit.getFileName() + " - Linking OK");
			}
		}

		System.out.println("\n--- Fase 4: Análise ---");
		for (CompilationUnit unit : validUnits) {
			if (verbose) {
				System.out.println("  Analisando: " + unit.getFileName());
			}

			Analyzer analyzer = new Analyzer(symbolTable, unit);
			analyzer.analyze();

			if (unit.hasErrors("ANALYZER")) {
				System.out.println("  ✗ " + unit.getFileName() + " - Falhou na análise");
				for (CompilationException e : unit.getErrors("ANALYZER")) {
					System.err.println("      " + e.getMessage());
				}
			} else {
				System.out.println("  ✓ " + unit.getFileName() + " - Análise OK");
			}

			unit.printReport();
			System.out.println();
		}
	}
}