package com.lang;

import com.lang.analyzer.ClassAnalyzer;
import com.lang.lexer.Lexer;
import com.lang.parser.Parser;
import com.lang.source.SourceFile;
import com.lang.symbol.SymbolTable;
import com.lang.tools.Printer;
import com.lang.unit.CompilationUnit;
import com.lang.unit.CompilationException;
import com.lang.unit.WarningException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
	private static boolean verbose = true;
	private static CompilationUnit sharedUnit = new CompilationUnit();
	private static SymbolTable symbolTable = new SymbolTable();
	private static ClassAnalyzer analyzer = new ClassAnalyzer(symbolTable, sharedUnit);
	private static String currentDir = System.getProperty("user.dir");
	private static final List<CommandHandler> commandHandlers = new ArrayList<>();

	public static void main(String[] args) {
		currentDir += "/tests";
		registerCommandHandlers();

		try {
			runInteractive();
		} catch (Exception e) {
			System.err.println("Erro: " + e.getMessage());
			if (verbose)
				e.printStackTrace();
			System.exit(1);
		}
	}

	private static void registerCommandHandlers() {
		commandHandlers.add(new ParseCommand());
		commandHandlers.add(new AnalyzeCommand());
		commandHandlers.add(new ClassesCommand());
		commandHandlers.add(new StatusCommand());
		commandHandlers.add(new ResetCommand());
		commandHandlers.add(new VerboseCommand());
		commandHandlers.add(new HelpCommand());
		commandHandlers.add(new ExitCommand());
	}

	private static void runInteractive() throws Exception {
		System.out.println("Lang CLI - Analisador de Classes");
		System.out.println("Comandos: parse, analyze, classes, status, reset, verbose, help, exit");
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.print("> ");
			String line = reader.readLine();
			if (line == null)
				break;

			line = line.trim();
			if (line.isEmpty())
				continue;

			boolean handled = false;
			for (CommandHandler handler : commandHandlers) {
				if (handler.canHandle(line)) {
					handled = handler.execute(line);
					break;
				}
			}

			if (!handled) {
				System.out.println("Comando inválido. Use 'help'");
			}
		}
	}

	// Command Handler Interface
	interface CommandHandler {
		boolean canHandle(String input);

		boolean execute(String input);

		default void help(StringBuilder sb) {
			sb.append("  ").append(getDescription()).append("\n");
		}

		String getDescription();
	}

	// Command Implementations
	static class ParseCommand implements CommandHandler {
		@Override
		public boolean canHandle(String input) {
			return input.startsWith("parse ");
		}

		@Override
		public boolean execute(String input) {
			String file = input.substring(6).trim();
			processFile(file);
			return true;
		}

		@Override
		public String getDescription() {
			return "parse <file> - Parseia arquivo .lang";
		}
	}

	static class AnalyzeCommand implements CommandHandler {
		@Override
		public boolean canHandle(String input) {
			return input.equals("analyze");
		}

		@Override
		public boolean execute(String input) {
			runAnalysis();
			return true;
		}

		@Override
		public String getDescription() {
			return "analyze - Analisa classes carregadas";
		}
	}

	static class ClassesCommand implements CommandHandler {
		@Override
		public boolean canHandle(String input) {
			return input.equals("classes");
		}

		@Override
		public boolean execute(String input) {
			printClasses();
			return true;
		}

		@Override
		public String getDescription() {
			return "classes - Lista classes carregadas";
		}
	}

	static class StatusCommand implements CommandHandler {
		@Override
		public boolean canHandle(String input) {
			return input.equals("status");
		}

		@Override
		public boolean execute(String input) {
			printStatus();
			return true;
		}

		@Override
		public String getDescription() {
			return "status - Mostra status atual";
		}
	}

	static class ResetCommand implements CommandHandler {
		@Override
		public boolean canHandle(String input) {
			return input.equals("reset");
		}

		@Override
		public boolean execute(String input) {
			reset();
			return true;
		}

		@Override
		public String getDescription() {
			return "reset - Reseta tudo";
		}
	}

	static class VerboseCommand implements CommandHandler {
		@Override
		public boolean canHandle(String input) {
			return input.equals("verbose");
		}

		@Override
		public boolean execute(String input) {
			verbose = !verbose;
			System.out.println("verbose = " + verbose);
			return true;
		}

		@Override
		public String getDescription() {
			return "verbose - Alterna modo verbose";
		}
	}

	static class HelpCommand implements CommandHandler {
		@Override
		public boolean canHandle(String input) {
			return input.equals("help") || input.equals("?");
		}

		@Override
		public boolean execute(String input) {
			StringBuilder sb = new StringBuilder();
			sb.append("\nComandos disponíveis:\n");
			for (CommandHandler handler : commandHandlers) {
				handler.help(sb);
			}
			sb.append("\n");
			System.out.print(sb.toString());
			return true;
		}

		@Override
		public String getDescription() {
			return "help - Mostra esta ajuda";
		}
	}

	static class ExitCommand implements CommandHandler {
		@Override
		public boolean canHandle(String input) {
			return input.equals("exit") || input.equals("quit");
		}

		@Override
		public boolean execute(String input) {
			System.exit(0);
			return true;
		}

		@Override
		public String getDescription() {
			return "exit - Sai do programa";
		}
	}

	// Helper Methods
	private static void printStatus() {
		System.out.println("Classes: " + sharedUnit.getClassCount());
		System.out.println("Erros: " + sharedUnit.getErrorCount());
		System.out.println("Avisos: " + sharedUnit.getWarningCount());
	}

	private static void printClasses() {
		if (sharedUnit.getClassCount() == 0) {
			System.out.println("Nenhuma classe carregada.");
			return;
		}

		System.out.println("\n=== Classes Carregadas (" + sharedUnit.getClassCount() + ") ===");
		sharedUnit.forEachClass(classDecl -> {
			System.out.println("  " + classDecl.name.source);
			if (verbose) {
				Printer printer = new Printer();
				System.out.println(printer.print(classDecl));
			}
		});
	}

	private static void runAnalysis() {
		if (sharedUnit.getClassCount() == 0) {
			System.out.println("Nenhuma classe para analisar.");
			return;
		}

		System.out.println("Executando análise...");
		analyzer.analyze();

		if (sharedUnit.hasErrors("ANALYZER")) {
			System.out.println("✗ Análise falhou:");
			for (CompilationException e : sharedUnit.getErrors("ANALYZER")) {
				System.err.println("  " + e.getMessage());
			}
		} else {
			System.out.println("Análise completa, nenhum erro encontrado");
		}

		sharedUnit.printReport();
	}

	private static void reset() {
		sharedUnit = new CompilationUnit();
		symbolTable = new SymbolTable();
		analyzer = new ClassAnalyzer(symbolTable, sharedUnit);
		System.out.println("Resetado");
	}

	private static void processFile(String path) {
		try {
			Path input = Paths.get(currentDir).resolve(path).normalize();

			if (!Files.exists(input)) {
				System.err.println("Arquivo não encontrado: " + input);
				return;
			}

			if (!input.toString().endsWith(".lang")) {
				System.err.println("Arquivo deve ter extensão .lang");
				return;
			}

			if (verbose)
				System.out.println("Parsing: " + input);

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
			if (verbose)
				e.printStackTrace();
		}
	}
}