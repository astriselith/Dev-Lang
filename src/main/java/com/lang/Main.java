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
                if (line == null) break;

                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equals("exit") || line.equals("quit")) break;

                if (line.equals("help") || line.equals("?")) {
                    printHelp();
                    continue;
                }

                if (line.startsWith("file ")) {
                    handleFileCommand(line.substring(5).trim());
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
        System.out.println("  file <arquivo1.lang> [<arquivo2.lang> ...] [--stub <arquivo.lang>]");
        System.out.println("      Analisa arquivos e mostra diagnósticos");
        System.out.println("      --stub <arquivo.lang> declarações externas disponíveis durante a análise");
        System.out.println();
        System.out.println("  help  - Mostra esta ajuda");
        System.out.println("  exit  - Sai do programa");
        System.out.println();
    }

    /* ===================== file ===================== */

    private static void handleFileCommand(String filesPart) {
        if (filesPart.isEmpty()) {
            System.err.println("Especifique pelo menos um arquivo");
            return;
        }
        String[] parts = filesPart.split("\\s+");
        Path stubPath = null;
        List<String> fileNames = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("--stub")) {
                if (i + 1 >= parts.length) {
                    System.err.println("--stub requer um arquivo");
                    return;
                }
                stubPath = Paths.get(parts[++i]);
            } else {
                fileNames.add(parts[i]);
            }
        }
        if (fileNames.isEmpty()) {
            System.err.println("Especifique pelo menos um arquivo");
            return;
        }

        List<CompilationUnit> units = new ArrayList<>();

        for (String fileName : fileNames) {
            Path path = Paths.get(fileName);
            if (!Files.exists(path)) {
                System.err.println("Arquivo não encontrado: " + path);
                continue;
            }
            units.add(new CompilationUnit(path.toString(), path.getFileName().toString()));
        }

        CompilationUnit stub = null;
        if (stubPath != null) {
            if (!Files.exists(stubPath)) {
                System.err.println("Arquivo de símbolos não encontrado: " + stubPath);
                return;
            }
            stub = new CompilationUnit(stubPath.toString(), stubPath.getFileName().toString());
        }

        if (!units.isEmpty()) processFiles(units, stub);
    }

    private static void processFiles(List<CompilationUnit> units, CompilationUnit stub) {
        // Parse
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

        // 1) Registro — cria símbolos, sem resolver tipos.
        for (CompilationUnit unit : all)
            new Register(table, unit).register();

        // 2) Linking — resolve tipos; roda após TODOS os registros,
        //    para que arquivos e stub se enxerguem independentemente da ordem.
        for (CompilationUnit unit : all)
            new Linker(table, unit).link();

        // 3) Análise semântica — assume linking completo.
        //    O stub só serve de referência; não é analisado semanticamente.
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
        return !hasAnyErrors(unit);
    }

    /* ===================== helpers ===================== */

    private static boolean hasAnyErrors(CompilationUnit unit) {
        return unit.getDiagnostics().values().stream()
                .flatMap(List::stream)
                .anyMatch(Diagnostic::isError);
    }
}