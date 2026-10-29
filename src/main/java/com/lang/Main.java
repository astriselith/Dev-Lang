package com.lang;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.lang.lexer.Lexer;
import com.lang.parser.Parser;
import com.lang.source.Source;
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
    }

    private static void processFiles(List<CompilationUnit> units) {
        for (CompilationUnit unit : units) {
            parse(unit);
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