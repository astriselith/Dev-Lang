package com.lang;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.lang.analyzer.Analyzer;
import com.lang.lexer.Lexer;
import com.lang.linker.Linker;
import com.lang.parser.Parser;
import com.lang.register.Register;
import com.lang.source.Source;
import com.lang.symbol.SymbolTable;
import com.lang.unit.CompilationUnit;
import com.lang.util.Diagnostic;

public class Main {

    public static void main(String[] args) throws Exception {
        runInteractive();
    }

    private static void runInteractive() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            try {
                System.out.print("> ");
                String line = reader.readLine();
                if (line == null)
                    break;

                line = line.trim();
                if (line.isEmpty())
                    continue;

                String[] tokens = line.split("\\s+");
                String command = tokens[0];
                String[] arguments = Arrays.copyOfRange(tokens, 1, tokens.length);

                if (!dispatch(command, arguments))
                    break;
            } catch (Exception e) {
                System.err.println("Erro: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static boolean dispatch(String command, String[] arguments) {
        if (command.equals("exit"))
            return false;

        if (command.equals("help")) {
            printHelp();
            return true;
        }

        if (command.equals("file")) {
            handleFileCommand(arguments);
            return true;
        }

        System.err.println("Comando inválido. Use 'help'");
        return true;
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("  file <arquivo1.lang> [<arquivo2.lang> ...] [--stub <arquivo.lang>]");
        System.out.println("      Analisa arquivos e mostra diagnósticos");
        System.out.println("      --stub <arquivo.lang> declarações externas disponíveis durante a análise");
        System.out.println();
        System.out.println("  help  - Mostra esta ajuda");
        System.out.println("  exit  - Sai do programa");
        System.out.println();
    }

    private static void handleFileCommand(String[] arguments) {
        if (arguments.length == 0) {
            System.err.println("Especifique pelo menos um arquivo");
            return;
        }

        CompilationUnit stub = null;
        List<CompilationUnit> units = new ArrayList<>();

        Iterator<String> fileIterator = Arrays.asList(arguments).iterator();
        while (fileIterator.hasNext()) {
            String argument = fileIterator.next();

            if (argument.equals("--stub")) {
                if (!fileIterator.hasNext()) {
                    System.err.println("--stub requer um arquivo");
                    return;
                }
                Path stubPath = Paths.get(fileIterator.next());
                if (!Files.exists(stubPath)) {
                    System.err.println("Arquivo de símbolos não encontrado: " + stubPath);
                    return;
                }
                if (stub != null) {
                    System.err.println("--stub especificado mais de uma vez");
                    return;
                }
                stub = new CompilationUnit(stubPath.toString(), stubPath.getFileName().toString());
                continue;
            }

            Path path = Paths.get(argument);
            if (!Files.exists(path)) {
                System.err.println("Arquivo não encontrado: " + path);
                continue;
            }
            units.add(new CompilationUnit(path.toString(), path.getFileName().toString()));
        }

        if (units.isEmpty()) {
            System.err.println("Nenhum arquivo válido para analisar");
            return;
        }

        processFiles(units, stub);
    }

    private static void processFiles(List<CompilationUnit> units, CompilationUnit stub) {
        boolean validStub = stub != null && parse(stub);
        List<CompilationUnit> validUnits = new ArrayList<>();
        for (CompilationUnit unit : units) {
            if (parse(unit))
                validUnits.add(unit);
        }

        SymbolTable table = new SymbolTable();

        List<CompilationUnit> all = new ArrayList<>();
        if (validStub)
            all.add(stub);
        all.addAll(validUnits);

        for (CompilationUnit unit : all)
            new Register(table, unit).register();

        for (CompilationUnit unit : all)
            new Linker(table, unit).link();

        for (CompilationUnit unit : validUnits)
            new Analyzer(table, unit).analyze();

        for (CompilationUnit unit : units)
            unit.printReport(Diagnostic.HINT);
        if (stub != null)
            stub.printReport(Diagnostic.ERROR);
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
        return !unit.hasDiagnostics(Diagnostic.ERROR);
    }
}