package com.lang.ls;

import com.lang.analyzer.Analyzer;
import com.lang.analyzer.AnalyzingErrorCode;
import com.lang.lexer.Lexer;
import com.lang.linker.Linker;
import com.lang.parser.Parser;
import com.lang.register.Register;
import com.lang.source.Source;
import com.lang.symbol.SymbolTable;
import com.lang.unit.CompilationUnit;
import com.lang.util.Diagnostic;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LangServer implements LanguageServer, LanguageClientAware,
        TextDocumentService, WorkspaceService {

    private static final String STUB = """
            class Any() {
                fun print(str: Stringfiable): Void;
                fun println(str: Stringfiable): Void;
            };
            class Void() : Any();
            class Null() : Void();
            class Bool() : Stringfiable();
            class Float() : Stringfiable();
            class Int() : Stringfiable();
            class String() : Stringfiable() {
                fun stringfy(): String;
            };
            class StringBuilder() : Stringfiable() {
                var str: String = "";
                fun stringfy(): String;
                fun append(str: Stringfiable): Void;
            };
            class Stringfiable() : Any() {
                fun stringfy(): String;
            };
            """;

    private final CompilationUnit stubUnit;

    private LanguageClient client;
    private final Map<String, String> docs = new HashMap<>();
    private final Map<String, CompilationUnit> cache = new HashMap<>();

    public LangServer() {
        this.stubUnit = parseStub();
    }

    // ==================== PONTO DE ENTRADA ====================

    public static void main(String[] args) throws Exception {
        LangServer server = new LangServer();
        Launcher<LanguageClient> launcher = Launcher.createLauncher(
                server, LanguageClient.class, System.in, System.out);
        server.connect(launcher.getRemoteProxy());
        launcher.startListening().get();
    }

    private static CompilationUnit parseStub() {
        CompilationUnit unit = new CompilationUnit("<stub>", "<stub>");
        try {
            Parser parser = new Parser(
                    new Lexer(Source.fromString(STUB, unit), unit), unit);
            parser.parse();
        } catch (Throwable t) {
            throw new IllegalStateException("Falha ao parsear o builtin stub", t);
        }
        return unit;
    }

    // ==================== LSP LIFECYCLE ====================

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities caps = new ServerCapabilities();
        caps.setTextDocumentSync(TextDocumentSyncKind.Full);
        caps.setInlayHintProvider(true);
        return CompletableFuture.completedFuture(new InitializeResult(caps));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return this;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return this;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
    }

    // ==================== TEXT DOCUMENT SYNC ====================

    @Override
    public void didOpen(DidOpenTextDocumentParams p) {
        docs.put(p.getTextDocument().getUri(), p.getTextDocument().getText());
        validate(p.getTextDocument().getUri());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams p) {
        if (!p.getContentChanges().isEmpty()) {
            docs.put(p.getTextDocument().getUri(),
                    p.getContentChanges().get(0).getText());
            validate(p.getTextDocument().getUri());
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams p) {
        docs.remove(p.getTextDocument().getUri());
        cache.remove(p.getTextDocument().getUri());
        if (client != null) {
            client.publishDiagnostics(new PublishDiagnosticsParams(
                    p.getTextDocument().getUri(), List.of()));
        }
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        // didChange já recompila a cada tecla; nada a fazer ao salvar.
        // Se um dia houver análise incremental, este é o ponto para
        // forçar um full recompile de segurança.
    }

    // ==================== WORKSPACE ====================

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        // Ponto de leitura de settings do cliente no futuro.
        // Quando houver settings (ex.: lang.inlayHints.enabled),
        // atualize as flags internas e re-valide tudo:
        //
        // for (String uri : docs.keySet()) validate(uri);
        //
        // Hoje: no-op.
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        // Dispara quando um arquivo muda fora do editor (git checkout,
        // gerador, etc.). Só recompilamos os que estão abertos — os
        // fechados não têm estado em cache para invalidar.
        for (FileEvent event : params.getChanges()) {
            String uri = event.getUri();
            if (docs.containsKey(uri)) {
                validate(uri);
            }
        }
    }

    // ==================== PIPELINE ====================

    private CompilationUnit compile(String uri) {
        CompilationUnit unit = new CompilationUnit(uri, uri);
        try {
            SymbolTable table = new SymbolTable();

            new Register(table, stubUnit).register();
            new Linker(table, stubUnit).link();

            Source source = Source.fromString(docs.getOrDefault(uri, ""), unit);
            Parser parser = new Parser(new Lexer(source, unit), unit);
            parser.parse();

            boolean parseFailed = unit.getDiagnostics("PARSER").stream()
                    .anyMatch(Diagnostic::isError);

            if (!parseFailed) {
                new Register(table, unit).register();
                new Linker(table, unit).link();
                new Analyzer(table, unit).analyze();
            }
        } catch (Throwable t) {
            // já virou Diagnostic dentro de `unit`
        }
        cache.put(uri, unit);
        return unit;
    }

    private void validate(String uri) {
        CompilationUnit unit = compile(uri);
        List<org.eclipse.lsp4j.Diagnostic> out = new ArrayList<>();

        for (Map.Entry<String, List<Diagnostic>> entry : unit.getDiagnostics().entrySet()) {
            boolean isAnalyzer = AnalyzingErrorCode.TAG.equals(entry.getKey());
            for (Diagnostic d : entry.getValue()) {
                // Hints do Analyzer viram inlay hints — evita duplicar em Problems.
                if (isAnalyzer && d.isHint())
                    continue;
                out.add(toLspDiagnostic(d));
            }
        }

        if (client != null) {
            client.publishDiagnostics(new PublishDiagnosticsParams(uri, out));
        }
    }

    // ==================== DIAGNOSTICS ====================

    private org.eclipse.lsp4j.Diagnostic toLspDiagnostic(Diagnostic d) {
        Range range;
        if (d.getPosition() == null || d.getPosition().getFirstLineCount() <= 0) {
            range = new Range(new Position(0, 0), new Position(0, 0));
        } else {
            range = toLspRange(d.getPosition());
        }
        return new org.eclipse.lsp4j.Diagnostic(
                range, d.getMessage(), toSeverity(d.getSeverity()), "lang");
    }

    private Range toLspRange(com.lang.util.Position p) {
        int startLine = Math.max(0, p.getFirstLineCount() - 1);
        int endLine = Math.max(0, p.getLastLineCount() - 1);
        int startCol = Math.max(0, p.getColumnStart());
        int endCol = Math.max(0, p.getColumnEnd());
        return new Range(
                new Position(startLine, startCol),
                new Position(endLine, endCol));
    }

    private DiagnosticSeverity toSeverity(int sev) {
        return switch (sev) {
            case Diagnostic.ERROR -> DiagnosticSeverity.Error;
            case Diagnostic.WARNING -> DiagnosticSeverity.Warning;
            case Diagnostic.INFO -> DiagnosticSeverity.Information;
            default -> DiagnosticSeverity.Hint;
        };
    }

    // ==================== INLAY HINTS ====================

    @Override
    public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
        CompilationUnit unit = cache.get(params.getTextDocument().getUri());
        if (unit == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        Range range = params.getRange();
        int startLine = range.getStart().getLine();
        int endLine = range.getEnd().getLine();

        List<InlayHint> out = new ArrayList<>();

        for (Diagnostic d : unit.getDiagnostics(AnalyzingErrorCode.TAG)) {
            if (!d.isHint())
                continue;

            com.lang.util.Position p = d.getPosition();
            if (p == null || p.getFirstLineCount() <= 0)
                continue;

            int line = p.getFirstLineCount() - 1;
            if (line < startLine || line > endLine)
                continue;

            int col = p.getColumnStart();

            InlayHint hint = new InlayHint(
                    new Position(line, col),
                    Either.forLeft(d.getMessage()));
            hint.setPaddingRight(true);
            // kind omitido — cliente escolhe o padrão
            out.add(hint);
        }

        out.sort(Comparator
                .comparingInt((InlayHint h) -> h.getPosition().getLine())
                .thenComparingInt(h -> h.getPosition().getCharacter()));

        return CompletableFuture.completedFuture(out);
    }
}