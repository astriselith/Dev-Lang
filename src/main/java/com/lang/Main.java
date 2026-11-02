package com.lang;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.lang.analyzer.Analyzer;
import com.lang.codegen.Codegen;
import com.lang.lexer.Lexer;
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

                if (line.startsWith("generate ") || line.startsWith("gen ")) {
                    String rest = line.startsWith("generate ")
                            ? line.substring(9).trim()
                            : line.substring(4).trim();
                    handleGenerateCommand(rest);
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
        System.out.println("  generate <arquivo1.lang> [<arquivo2.lang> ...] [--stub <arquivo.lang>] [--out <dir>]");
        System.out.println("      Gera <arquivo>.h e <arquivo>.c para cada fonte");
        System.out.println("      --stub <arquivo.lang> arquivo de declarações externas usado como referência de símbolos");
        System.out.println("      --out <dir>   diretório de saída (default: ao lado do .lang)");
        System.out.println("      A geração valida os arquivos semanticamente e é interrompida se houver erros");
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
        boolean validStub = stub != null && parse(stub);
        List<CompilationUnit> validUnits = new ArrayList<>();
        for (CompilationUnit unit : units) {
            if (parse(unit))
                validUnits.add(unit);
        }

        SymbolTable table = new SymbolTable();
        Register stubRegister = validStub ? new Register(table, stub) : null;
        if (stubRegister != null)
            stubRegister.register();
        for (CompilationUnit unit : validUnits) {
            Register register = new Register(table, unit);
            register.register();
        }

        for (CompilationUnit unit : validUnits) {
            new Analyzer(table, unit).analyze();
        }

        for (CompilationUnit unit : units) unit.printReport(Diagnostic.HINT);
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

    /* ===================== generate ===================== */

    private static void handleGenerateCommand(String rest) {
        if (rest.isEmpty()) {
            System.err.println("Uso: generate <arquivo1.lang> [<arquivo2.lang> ...] [--out <dir>]");
            return;
        }

        Path outDir = null;
        Path stubPath = null;
        List<String> files = new ArrayList<>();
        String[] parts = rest.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("--out")) {
                if (i + 1 >= parts.length) {
                    System.err.println("--out requer um diretório");
                    return;
                }
                outDir = Paths.get(parts[++i]);
            } else if (parts[i].equals("--stub")) {
                if (i + 1 >= parts.length) {
                    System.err.println("--stub requer um arquivo");
                    return;
                }
                stubPath = Paths.get(parts[++i]);
            } else {
                files.add(parts[i]);
            }
        }

        if (files.isEmpty()) {
            System.err.println("Nenhum arquivo especificado");
            return;
        }

        if (outDir != null) {
            try {
                Files.createDirectories(outDir);
            } catch (Exception e) {
                System.err.println("Não foi possível criar " + outDir + ": " + e.getMessage());
                return;
            }
        }

        CompilationUnit symbols = null;
        if (stubPath != null) {
            if (!Files.exists(stubPath)) {
                System.err.println("Arquivo de símbolos não encontrado: " + stubPath);
                return;
            }
            try {
                symbols = parseUnit(stubPath);
            } catch (Exception e) {
                System.err.println("Não foi possível carregar símbolos de " + stubPath + ": " + e.getMessage());
                return;
            }
            if (hasAnyErrors(symbols)) {
                System.err.println("Stub inválido: " + stubPath);
                symbols.printReport(Diagnostic.ERROR);
                return;
            }
        }

        int ok = 0, fail = 0;
        for (String fileName : files) {
            if (generateFile(Paths.get(fileName), outDir, symbols)) ok++;
            else fail++;
        }
        System.out.println("Concluído: " + ok + " gerado(s), " + fail + " falha(s).");
    }

    private static boolean generateFile(Path src, Path outDir, CompilationUnit symbols) {
        if (!Files.exists(src)) {
            System.err.println("Arquivo não encontrado: " + src);
            return false;
        }

        Path targetDir = (outDir != null) ? outDir : src.toAbsolutePath().getParent();
        String base = src.getFileName().toString().replaceFirst("\\.[^.]+$", "");

        try {
            CompilationUnit unit = parseUnit(src);

            if (hasAnyErrors(unit)) {
                System.err.println("Não gerado (erros de parsing): " + src);
                unit.printReport(Diagnostic.ERROR);
                return false;
            }

            SymbolTable table = new SymbolTable();
            Register stubRegister = null;
            if (symbols != null) {
                stubRegister = new Register(table, symbols);
                stubRegister.register();
            }
            Register unitRegister = new Register(table, unit);
            unitRegister.register();
            if (symbols != null && hasAnyErrors(symbols)) {
                System.err.println("Não gerado (erros ao registrar o stub): "
                        + symbols.getFilePath());
                symbols.printReport(Diagnostic.ERROR);
                return false;
            }
            new Analyzer(table, unit).analyze();
            if (hasAnyErrors(unit)) {
                System.err.println("Não gerado (erros de análise): " + src);
                unit.printReport(Diagnostic.ERROR);
                return false;
            }

            Codegen gen = new Codegen(unit, table);
            String headerText = gen.header();
            String sourceText = gen.source();

            Path headerPath = targetDir.resolve(base + ".h");
            Path sourcePath = targetDir.resolve(base + ".c");

            Files.writeString(headerPath, headerText);
            Files.writeString(sourcePath, sourceText);

            System.out.println("Gerado: " + headerPath);
            System.out.println("        " + sourcePath);
            return true;
        } catch (Exception e) {
            System.err.println("Erro ao gerar " + src + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static CompilationUnit parseUnit(Path src) throws Exception {
        CompilationUnit unit = new CompilationUnit(
                src.toString(), src.getFileName().toString());
        Source source = Source.fromFile(src, unit);
        Lexer lexer = new Lexer(source, unit);
        Parser parser = new Parser(lexer, unit);
        parser.parse();
        return unit;
    }

    /* ===================== helpers ===================== */

    private static boolean hasAnyErrors(CompilationUnit unit) {
        return unit.getDiagnostics().values().stream()
                .flatMap(List::stream)
                .anyMatch(Diagnostic::isError);
    }
}