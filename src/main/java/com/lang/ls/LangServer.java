package com.lang.ls;

import static com.lang.analyzer.AnalyzingErrorCode.*;

import com.lang.analyzer.*;
import com.lang.ast.*;
import com.lang.codepoint.Codepoint;
import com.lang.lexer.*;
import com.lang.linker.*;
import com.lang.parser.*;
import com.lang.register.*;
import com.lang.source.*;
import com.lang.symbol.*;
import com.lang.unit.*;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.*;
import org.eclipse.lsp4j.jsonrpc.messages.*;
import org.eclipse.lsp4j.services.*;

import java.util.*;
import java.util.concurrent.*;

public class LangServer implements LanguageServer, LanguageClientAware,
        TextDocumentService, WorkspaceService {

    private static final String STUB = """
            # Classe raiz da hierarquia builtin. #
            class Any() {
                # Imprime uma Stringfiable sem quebra de linha. #
                fun print(str: Stringfiable): Void;

                # Imprime uma Stringfiable com quebra de linha. #
                fun println(str: Stringfiable): Void;
            };

            # Subclasse vazia de Any; representa ausência de valor. #
            class Void() : Any();

            # Subclasse vazia de Void; representa o literal null. #
            class Null() : Void();

            # Booleano; conversível para Stringfiable. #
            class Bool() : Stringfiable();

            # Ponto flutuante; conversível para Stringfiable. #
            class Float() : Stringfiable();

            # Inteiro; conversível para Stringfiable. #
            class Int() : Stringfiable();

            # String imutável. #
            class String() : Stringfiable() {
                # Retorna a própria string. #
                fun stringfy(): String;
            };

            # Construtor de string mutável. #
            class StringBuilder() : Stringfiable() {
                # Buffer interno. #
                var str: String = "";

                # Retorna o conteúdo acumulado. #
                fun stringfy(): String;

                # Anexa uma Stringfiable ao buffer. #
                fun append(str: Stringfiable): Void;
            };

            # Interface informal: qualquer tipo com stringfy(). #
            class Stringfiable() : Any() {
                # Representação textual do valor. #
                fun stringfy(): String;
            };
            """;

    private static final String PARSER_TAG = "PARSER";
    private static final String LANG_ID = "lang";
    private static final String HINT_DEFAULT_MARKER = "\u0001D";

    private final CompilationUnit stubUnit;
    private final SymbolTable stubTable;
    private volatile Map<Node, String> cachedStubDocs;

    private LanguageClient client;
    private final Map<String, String> docs = new HashMap<>();
    private final Map<String, CompilationUnit> cache = new HashMap<>();
    private final Map<String, SymbolTable> tables = new HashMap<>();

    private final Map<String, Map<Integer, String>> lineDocs = new HashMap<>();
    private final Map<String, Map<Integer, CommentEntry>> commentIndex = new HashMap<>();
    private final Map<String, Map<Integer, List<RefEntry>>> refIndex = new HashMap<>();
    private final Map<String, Map<Integer, List<TypeRefEntry>>> typeRefIndex = new HashMap<>();
    private final Map<String, Map<Integer, List<DeclNameEntry>>> declNameIndex = new HashMap<>();
    private final Map<String, Map<Integer, List<LiteralEntry>>> literalIndex = new HashMap<>();
    private final Map<String, Map<Node, String>> docByNode = new HashMap<>();

    private record CommentEntry(String text, Range range) {}

    private record RefEntry(
            int startCharacter,
            int endCharacter,
            Symbol symbol,
            String typeName,
            String signature) {}

    private record TypeRefEntry(int startCharacter, int endCharacter, Typed typed) {}

    private record DeclNameEntry(int startCharacter, int endCharacter, Symbol symbol) {}

    private record LiteralEntry(int startCharacter, int endCharacter, int literalType) {}

    public LangServer() {
        CompilationUnit unit = new CompilationUnit("<stub>", "<stub>");
        SymbolTable table = new SymbolTable();
        try {
            Parser parser = new Parser(
                    new Lexer(Source.fromString(STUB, unit), unit), unit);
            parser.parse();

            boolean parseFailed = unit.getDiagnostics(PARSER_TAG).stream()
                    .anyMatch(com.lang.util.Diagnostic::isError);

            if (!parseFailed) {
                new Register(table, unit).register();
                new Linker(table, unit).link();
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Falha ao parsear o builtin stub", t);
        }
        this.stubUnit = unit;
        this.stubTable = table;
    }

    public static void main(String[] args) throws Exception {
        LangServer server = new LangServer();
        Launcher<LanguageClient> launcher = Launcher.createLauncher(
                server, LanguageClient.class, System.in, System.out);
        server.connect(launcher.getRemoteProxy());
        launcher.startListening().get();
    }

    // ==================== LIFECYCLE ====================

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        ServerCapabilities caps = new ServerCapabilities();
        caps.setTextDocumentSync(TextDocumentSyncKind.Full);
        caps.setInlayHintProvider(true);
        caps.setHoverProvider(true);
        caps.setRenameProvider(true);
        caps.setSelectionRangeProvider(true);
        caps.setDocumentSymbolProvider(true);
        caps.setDefinitionProvider(true);
        return CompletableFuture.completedFuture(new InitializeResult(caps));
    }

    @Override public CompletableFuture<Object> shutdown() { return CompletableFuture.completedFuture(null); }
    @Override public void exit() { System.exit(0); }
    @Override public TextDocumentService getTextDocumentService() { return this; }
    @Override public WorkspaceService getWorkspaceService() { return this; }
    @Override public void connect(LanguageClient client) { this.client = client; }

    // ==================== SYNC ====================

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
        String uri = p.getTextDocument().getUri();
        docs.remove(uri);
        cache.remove(uri);
        tables.remove(uri);
        lineDocs.remove(uri);
        commentIndex.remove(uri);
        refIndex.remove(uri);
        typeRefIndex.remove(uri);
        declNameIndex.remove(uri);
        literalIndex.remove(uri);
        docByNode.remove(uri);
        if (client != null) {
            client.publishDiagnostics(new PublishDiagnosticsParams(uri, List.of()));
        }
    }

    @Override public void didSave(DidSaveTextDocumentParams params) { }
    @Override public void didChangeConfiguration(DidChangeConfigurationParams params) { }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        for (FileEvent event : params.getChanges()) {
            if (docs.containsKey(event.getUri())) validate(event.getUri());
        }
    }

    // ==================== PIPELINE ====================

    private CompilationUnit compile(String uri) {
        CompilationUnit unit = new CompilationUnit(uri, uri);
        SymbolTable table = new SymbolTable();
        try {
            new Register(table, stubUnit).register();
            new Linker(table, stubUnit).link();

            Source source = Source.fromString(docs.getOrDefault(uri, ""), unit);
            Parser parser = new Parser(new Lexer(source, unit), unit);
            parser.parse();

            boolean parseFailed = unit.getDiagnostics(PARSER_TAG).stream()
                    .anyMatch(com.lang.util.Diagnostic::isError);

            if (!parseFailed) {
                new Register(table, unit).register();
                new Linker(table, unit).link();
                new Analyzer(table, unit).analyze();
            }
        } catch (Throwable t) {
            // já virou Diagnostic dentro de `unit`
        }
        cache.put(uri, unit);
        tables.put(uri, table);
        return unit;
    }

    private void validate(String uri) {
        CompilationUnit unit = compile(uri);
        rebuildDocIndex(uri, unit);

        List<org.eclipse.lsp4j.Diagnostic> out = new ArrayList<>();
        for (var entry : unit.getDiagnostics().entrySet()) {
            boolean isAnalyzer = TAG.equals(entry.getKey());
            for (com.lang.util.Diagnostic d : entry.getValue()) {
                if (isAnalyzer && d.isHint()) continue;
                out.add(toLspDiagnostic(d));
            }
        }
        if (client != null) {
            client.publishDiagnostics(new PublishDiagnosticsParams(uri, out));
        }
    }

    // ==================== ÍNDICES ====================

    private void rebuildDocIndex(String uri, CompilationUnit unit) {
        Map<Integer, String> textByEndLine = new HashMap<>();
        Map<Integer, CommentEntry> byCommentStartLine = new HashMap<>();

        List<Comment> comments = unit.getComments();
        if (comments != null) {
            for (Comment c : comments) {
                if (c == null || c.text == null || c.position == null) continue;
                String clean = cleanComment(c.text);
                if (clean.isEmpty()) continue;

                int startLine0 = Math.max(0, c.position.getFirstLineCount() - 1);
                int endLine0   = Math.max(0, c.position.getLastLineCount()  - 1);
                int startCol   = Math.max(0, c.position.getColumnStart());
                int endCol     = Math.max(0, c.position.getColumnEnd());

                Range range = new Range(
                        new Position(startLine0, startCol),
                        new Position(endLine0, endCol));

                byCommentStartLine.put(startLine0, new CommentEntry(clean, range));
                textByEndLine.put(endLine0, clean);
            }
        }

        Map<Integer, String> byTargetLine = new HashMap<>();
        Map<Node, String> docsByNode = new IdentityHashMap<>();

        for (Decl decl : collectDeclarations(unit)) {
            if (decl == null || decl.symbol == null) continue;
            Symbol sym = decl.symbol;
            if (sym.node == null || sym.node.getPosition() == null) continue;

            int firstLine1 = sym.node.getPosition().getFirstLineCount();
            if (firstLine1 <= 0) continue;

            int targetLine0 = firstLine1 - 1;
            if (targetLine0 <= 0) continue;

            String doc = textByEndLine.get(targetLine0 - 1);
            if (doc != null) {
                byTargetLine.put(targetLine0, doc);
                docsByNode.put(sym.node, doc);
            }
        }

        lineDocs.put(uri, byTargetLine);
        commentIndex.put(uri, byCommentStartLine);
        docByNode.put(uri, docsByNode);

        Map<Integer, List<RefEntry>> refs = new HashMap<>();
        Map<Integer, List<TypeRefEntry>> typeRefs = new HashMap<>();
        Map<Integer, List<DeclNameEntry>> declNames = new HashMap<>();
        Map<Integer, List<LiteralEntry>> literals = new HashMap<>();
        for (ClassDecl cls : unit.getClasses()) {
            if (cls == null) continue;
            visitClassBody(cls, refs, typeRefs, declNames, literals);
        }
        refIndex.put(uri, refs);
        typeRefIndex.put(uri, typeRefs);
        declNameIndex.put(uri, declNames);
        literalIndex.put(uri, literals);
    }

    // ==================== COLETA DE DECLS ====================

    private static List<Decl> collectDeclarations(CompilationUnit unit) {
        List<Decl> out = new ArrayList<>();
        for (ClassDecl c : unit.getClasses()) collectClass(c, out);
        return out;
    }

    private static void collectClass(ClassDecl decl, List<Decl> out) {
        if (decl == null) return;
        out.add(decl);

        if (decl.typeParameters != null)
            for (TypeParamDecl tp : decl.typeParameters) collectTypeParam(tp, out);
        if (decl.parameters != null)
            for (ParamDecl p : decl.parameters) collectParam(p, out);
        if (decl.supers != null)
            for (SuperDecl s : decl.supers) if (s != null) out.add(s);
        if (decl.members != null)
            for (var m : decl.members) collectMember(m, out);
    }

    private static void collectMember(Decl decl, List<Decl> out) {
        if (decl == null) return;
        if (decl instanceof FunDecl fun) collectFun(fun, out);
        else if (decl instanceof VarDecl var) out.add(var);
        else out.add(decl);
    }

    private static void collectFun(FunDecl decl, List<Decl> out) {
        out.add(decl);
        if (decl.typeParameters != null)
            for (TypeParamDecl tp : decl.typeParameters) collectTypeParam(tp, out);
        if (decl.parameters != null)
            for (ParamDecl p : decl.parameters) collectParam(p, out);
    }

    private static void collectParam(ParamDecl decl, List<Decl> out) {
        if (decl != null) out.add(decl);
    }

    private static void collectTypeParam(TypeParamDecl decl, List<Decl> out) {
        if (decl == null) return;
        out.add(decl);
        if (decl.bounds != null)
            for (BoundDecl b : decl.bounds) if (b != null) out.add(b);
    }

    // ==================== TRAVERSAL ====================

    private void visitClassBody(ClassDecl cls,
                                Map<Integer, List<RefEntry>> refs,
                                Map<Integer, List<TypeRefEntry>> typeRefs,
                                Map<Integer, List<DeclNameEntry>> declNames,
                                Map<Integer, List<LiteralEntry>> literals) {
        addDeclName(cls.name, cls.symbol, declNames);

        if (cls.typeParameters != null) {
            for (TypeParamDecl tp : cls.typeParameters) {
                visitTypeParamDecl(tp, refs, typeRefs, declNames, literals);
            }
        }
        if (cls.parameters != null) {
            for (ParamDecl p : cls.parameters) {
                visitParamDecl(p, refs, typeRefs, declNames, literals);
            }
        }
        if (cls.supers != null) {
            for (SuperDecl s : cls.supers) visitSuperDecl(s, refs, typeRefs, literals);
        }
        if (cls.members != null) {
            for (var m : cls.members) {
                if (m instanceof FunDecl fun) visitFunDecl(fun, refs, typeRefs, declNames, literals);
                else if (m instanceof VarDecl var) visitVarDecl(var, refs, typeRefs, declNames, literals);
            }
        }
    }

    private void visitFunDecl(FunDecl fun,
                              Map<Integer, List<RefEntry>> refs,
                              Map<Integer, List<TypeRefEntry>> typeRefs,
                              Map<Integer, List<DeclNameEntry>> declNames,
                              Map<Integer, List<LiteralEntry>> literals) {
        addDeclName(fun.name, fun.symbol, declNames);
        if (fun.type != null) visitTyped(fun.type, typeRefs);
        if (fun.typeParameters != null) {
            for (TypeParamDecl tp : fun.typeParameters) {
                visitTypeParamDecl(tp, refs, typeRefs, declNames, literals);
            }
        }
        if (fun.parameters != null) {
            for (ParamDecl p : fun.parameters) {
                visitParamDecl(p, refs, typeRefs, declNames, literals);
            }
        }
        if (fun.body != null) {
            visitStmt(fun.body, refs, typeRefs, literals);
        }
    }

    private void visitVarDecl(VarDecl var,
                              Map<Integer, List<RefEntry>> refs,
                              Map<Integer, List<TypeRefEntry>> typeRefs,
                              Map<Integer, List<DeclNameEntry>> declNames,
                              Map<Integer, List<LiteralEntry>> literals) {
        addDeclName(var.name, var.symbol, declNames);
        if (var.type != null) visitTyped(var.type, typeRefs);
        if (var.value != null) visitExpr(var.value, refs, typeRefs, literals);
    }

    private void visitParamDecl(ParamDecl p,
                                Map<Integer, List<RefEntry>> refs,
                                Map<Integer, List<TypeRefEntry>> typeRefs,
                                Map<Integer, List<DeclNameEntry>> declNames,
                                Map<Integer, List<LiteralEntry>> literals) {
        addDeclName(p.name, p.symbol, declNames);
        if (p.type != null) visitTyped(p.type, typeRefs);
        if (p.init != null) visitExpr(p.init, refs, typeRefs, literals);
    }

    private void visitSuperDecl(SuperDecl s,
                                Map<Integer, List<RefEntry>> refs,
                                Map<Integer, List<TypeRefEntry>> typeRefs,
                                Map<Integer, List<LiteralEntry>> literals) {
        if (s.type != null) visitTyped(s.type, typeRefs);
        if (s.arguments != null) {
            for (Expr e : s.arguments) visitExpr(e, refs, typeRefs, literals);
        }
    }

    private void visitBoundDecl(BoundDecl b,
                                Map<Integer, List<RefEntry>> refs,
                                Map<Integer, List<TypeRefEntry>> typeRefs) {
        if (b.type != null) visitTyped(b.type, typeRefs);
    }

    private void visitTypeParamDecl(TypeParamDecl tp,
                                    Map<Integer, List<RefEntry>> refs,
                                    Map<Integer, List<TypeRefEntry>> typeRefs,
                                    Map<Integer, List<DeclNameEntry>> declNames,
                                    Map<Integer, List<LiteralEntry>> literals) {
        addDeclName(tp.name, tp.symbol, declNames);
        if (tp.bounds != null) {
            for (BoundDecl b : tp.bounds) visitBoundDecl(b, refs, typeRefs);
        }
    }

    private void visitStmt(Stmt stmt,
                           Map<Integer, List<RefEntry>> refs,
                           Map<Integer, List<TypeRefEntry>> typeRefs,
                           Map<Integer, List<LiteralEntry>> literals) {
        if (stmt instanceof BlockStmt b) {
            if (b.statements != null) for (Stmt s : b.statements) visitStmt(s, refs, typeRefs, literals);
        } else if (stmt instanceof LetStmt l) {
            if (l.type != null) visitTyped(l.type, typeRefs);
            if (l.value != null) visitExpr(l.value, refs, typeRefs, literals);
        } else if (stmt instanceof ExprStmt e) {
            if (e.expression != null) visitExpr(e.expression, refs, typeRefs, literals);
        } else if (stmt instanceof ReturnStmt r) {
            if (r.value != null) visitExpr(r.value, refs, typeRefs, literals);
        } else if (stmt instanceof IfStmt i) {
            if (i.condition != null) visitExpr(i.condition, refs, typeRefs, literals);
            if (i.thenBranch != null) visitStmt(i.thenBranch, refs, typeRefs, literals);
            if (i.elseBranch != null) visitStmt(i.elseBranch, refs, typeRefs, literals);
        } else if (stmt instanceof WhileStmt w) {
            if (w.condition != null) visitExpr(w.condition, refs, typeRefs, literals);
            if (w.body != null) visitStmt(w.body, refs, typeRefs, literals);
        }
    }

    private void visitExpr(Expr expr,
                           Map<Integer, List<RefEntry>> refs,
                           Map<Integer, List<TypeRefEntry>> typeRefs,
                           Map<Integer, List<LiteralEntry>> literals) {
        if (expr == null) return;

        if (expr instanceof LiteralExpr lit) {
            addLiteral(lit, literals);
            return;
        }

        if (expr instanceof RefExpr ref) {
            addRef(ref.name, ref.symbol, null, refs);
            return;
        }

        if (expr instanceof MemberAccessExpr m) {
            Symbol substitutedType = computeMemberAccessType(m);
            addRef(m.name, m.symbol, substitutedType, refs);
            visitExpr(m.object, refs, typeRefs, literals);
            return;
        }

        if (expr instanceof CallExpr c) {
            visitExpr(c.callee, refs, typeRefs, literals);
            if (c.typeArguments != null) {
                for (Typed t : c.typeArguments) visitTyped(t, typeRefs);
            }
            if (c.arguments != null) {
                for (Expr a : c.arguments) visitExpr(a, refs, typeRefs, literals);
            }
            return;
        }

        if (expr instanceof BinaryExpr b) {
            visitExpr(b.left, refs, typeRefs, literals);
            visitExpr(b.right, refs, typeRefs, literals);
        } else if (expr instanceof UnaryExpr u) {
            visitExpr(u.operand, refs, typeRefs, literals);
        } else if (expr instanceof AssignExpr a) {
            visitExpr(a.target, refs, typeRefs, literals);
            visitExpr(a.value, refs, typeRefs, literals);
        } else if (expr instanceof ParenthesisExpr p) {
            visitExpr(p.inner, refs, typeRefs, literals);
        }
    }

    // ==================== SUBSTITUIÇÃO DE MEMBER ACCESS ====================

    private Symbol computeMemberAccessType(MemberAccessExpr m) {
        if (m == null || m.symbol == null) return null;
        if (!m.symbol.isMember()) return null;

        Symbol memberType = m.symbol.asMember().getType();
        if (memberType == null) return null;

        Symbol receiverType = receiverTypeOf(m.object);
        if (receiverType == null || !receiverType.isParameterizedClass()) {
            return memberType;
        }

        Map<String, Symbol> subs =
                receiverType.asParameterizedClass().getTypeArguments();
        return substitute(memberType, subs);
    }

    private Symbol receiverTypeOf(Expr e) {
        if (e == null || e.symbol == null) return null;

        Symbol sym = e.symbol;
        if (sym.isMember()) return sym.asMember().getType();
        if (sym.isParam()) return sym.asParam().getType();
        if (sym.isClass()) return sym;
        if (sym.isParameterizedClass()) return sym;
        if (sym.isTypeParam()) return sym;
        return null;
    }

    private Symbol substitute(Symbol type, Map<String, Symbol> subs) {
        if (type == null || subs == null || subs.isEmpty()) return type;

        if (type.isTypeParam()) {
            Symbol replacement = subs.get(type.getName());
            return replacement != null ? replacement : type;
        }

        if (type.isParameterizedClass()) {
            ParameterizedClassSymbol pc = type.asParameterizedClass();
            Map<String, Symbol> newArgs = new LinkedHashMap<>();
            for (Map.Entry<String, Symbol> e : pc.getTypeArguments().entrySet()) {
                newArgs.put(e.getKey(), substitute(e.getValue(), subs));
            }
            return new ParameterizedClassSymbol(pc.getBase(), newArgs);
        }

        return type;
    }

    // ==================== ADD REF / DECL NAME / TYPE REF / LITERAL ====================

    private void addRef(Identifier id, Symbol symbol, Symbol typeOverride,
                        Map<Integer, List<RefEntry>> refs) {
        if (id == null || id.position == null) return;

        int line0 = Math.max(0, id.position.getFirstLineCount() - 1);
        int startCol = Math.max(0, id.position.getColumnStart());
        int endCol = Math.max(0, id.position.getColumnEnd());
        if (endCol <= startCol) {
            endCol = startCol + Math.max(1, id.source != null ? id.source.length() : 1);
        }

        String typeName = (typeOverride != null)
                ? renderTypeWithBounds(typeOverride)
                : renderSymbolType(symbol);
        String signature = renderSignature(symbol);
        refs.computeIfAbsent(line0, k -> new ArrayList<>())
                .add(new RefEntry(startCol, endCol, symbol, typeName, signature));
    }

    private void addDeclName(Identifier id, Symbol symbol,
                             Map<Integer, List<DeclNameEntry>> declNames) {
        if (id == null || id.position == null || symbol == null) return;

        int line0 = Math.max(0, id.position.getFirstLineCount() - 1);
        int startCol = Math.max(0, id.position.getColumnStart());
        int endCol = Math.max(0, id.position.getColumnEnd());
        if (endCol <= startCol) {
            endCol = startCol + Math.max(1, id.source != null ? id.source.length() : 1);
        }

        declNames.computeIfAbsent(line0, k -> new ArrayList<>())
                .add(new DeclNameEntry(startCol, endCol, symbol));
    }

    private void visitTyped(Typed type, Map<Integer, List<TypeRefEntry>> typeRefs) {
        if (type == null || type.position == null) return;

        if (type instanceof RefTyped ref) {
            addTypeRef(ref.name, ref, typeRefs);
        } else if (type instanceof ParameterizedRefTyped pref) {
            addTypeRef(pref.name, pref, typeRefs);
            if (pref.typeArguments != null) {
                for (Typed arg : pref.typeArguments) visitTyped(arg, typeRefs);
            }
        }
    }

    private void addTypeRef(Identifier id, Typed typed, Map<Integer, List<TypeRefEntry>> typeRefs) {
        if (id == null || id.position == null) return;

        int line0 = Math.max(0, id.position.getFirstLineCount() - 1);
        int startCol = Math.max(0, id.position.getColumnStart());
        int endCol = Math.max(0, id.position.getColumnEnd());
        if (endCol <= startCol) {
            endCol = startCol + Math.max(1, id.source != null ? id.source.length() : 1);
        }

        typeRefs.computeIfAbsent(line0, k -> new ArrayList<>())
                .add(new TypeRefEntry(startCol, endCol, typed));
    }

    private void addLiteral(LiteralExpr lit, Map<Integer, List<LiteralEntry>> literals) {
        if (lit == null || lit.position == null) return;

        int line0 = Math.max(0, lit.position.getFirstLineCount() - 1);
        int startCol = Math.max(0, lit.position.getColumnStart());
        int endCol = Math.max(0, lit.position.getColumnEnd());
        if (endCol <= startCol) {
            endCol = startCol + 1;
        }

        literals.computeIfAbsent(line0, k -> new ArrayList<>())
                .add(new LiteralEntry(startCol, endCol, lit.type));
    }

    // ==================== UTIL ====================

    private static String cleanComment(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String s = raw;
        if (s.startsWith("#")) s = s.substring(1);
        if (s.endsWith("#")) s = s.substring(0, s.length() - 1);
        return s.strip();
    }

    private String renderSymbolType(Symbol sym) {
        if (sym == null) return null;

        Symbol t;
        if (sym.isMember()) t = sym.asMember().getType();
        else if (sym.isParam()) t = sym.asParam().getType();
        else if (sym.isSuper()) t = sym.asSuper().getType();
        else if (sym.isBound()) t = sym.asBound().getType();
        else t = sym;

        return renderTypeWithBounds(t);
    }

    private String renderTypeWithBounds(Symbol t) {
        if (t == null) return null;

        if (t.isTypeParam()) {
            TypeParamSymbol tp = t.asTypeParam();
            if (!tp.getBounds().isEmpty()) {
                StringBuilder sb = new StringBuilder(tp.getName()).append(": ");
                boolean first = true;
                for (BoundSymbol b : tp.getBounds().values()) {
                    Symbol bt = b.getType();
                    if (bt == null) continue;
                    if (!first) sb.append(" & ");
                    first = false;
                    sb.append(renderType(bt));
                }
                if (first) {
                    return tp.getName();
                }
                return sb.toString();
            }
            return tp.getName();
        }

        return renderType(t);
    }

    private String renderType(Symbol t) {
        if (t == null) return null;

        if (t.isClass()) return t.asClass().getName();
        if (t.isParameterizedClass()) {
            var pcs = t.asParameterizedClass();
            StringBuilder sb = new StringBuilder(pcs.getBase().getName()).append("<");
            boolean first = true;
            for (var e : pcs.getTypeArguments().entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(renderType(e.getValue()));
            }
            return sb.append(">").toString();
        }
        if (t.isTypeParam()) return t.getName();
        if (t.isFun()) {
            Symbol ret = t.asFun().getType();
            return ret != null ? renderType(ret) : "Void";
        }
        if (t.isVar()) return t.asVar().getType() != null
                ? renderType(t.asVar().getType())
                : "var";
        if (t.isParam()) return t.asParam().getType() != null
                ? renderType(t.asParam().getType())
                : "param";
        if (t.isSuper()) return t.asSuper().getType() != null
                ? renderType(t.asSuper().getType())
                : "super";
        if (t.isBound()) return t.asBound().getType() != null
                ? renderType(t.asBound().getType())
                : "bound";
        return t.getName();
    }

    private static String baseTypeName(String typeText) {
        if (typeText == null) return null;
        int lt = typeText.indexOf('<');
        String base = lt < 0 ? typeText : typeText.substring(0, lt);
        return base.isBlank() ? null : base.trim();
    }

    private String renderSignature(Symbol sym) {
        if (sym == null) return null;

        if (sym.isFun()) {
            return renderFunctionSignature(sym.asFun());
        }
        if (sym.isClass()) {
            return renderClassSignature(sym.asClass());
        }
        return null;
    }

    private String renderFunctionSignature(FunSymbol fun) {
        if (fun == null) return null;

        StringBuilder sb = new StringBuilder("fun ")
                .append(fun.getName());

        if (!fun.getTypeParameters().isEmpty()) {
            sb.append("<");
            boolean first = true;
            for (String tpName : fun.getTypeParameters().keySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(renderTypeParamDecl(fun.getTypeParameter(tpName)));
            }
            sb.append(">");
        }

        sb.append("(");
        boolean first = true;
        for (ParamSymbol p : fun.getParameters().values()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(p.getName()).append(": ");
            String t = p.getType() != null ? renderType(p.getType()) : "?";
            sb.append(t);
        }

        sb.append("): ");
        Symbol ret = fun.getType();
        sb.append(ret != null ? renderType(ret) : "Void");

        return sb.toString();
    }

    private String renderClassSignature(ClassSymbol cls) {
        if (cls == null) return null;

        StringBuilder sb = new StringBuilder("class ")
                .append(cls.getName());

        if (!cls.getTypeParameters().isEmpty()) {
            sb.append("<");
            boolean first = true;
            for (String tpName : cls.getTypeParameters().keySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(renderTypeParamDecl(cls.getTypeParameter(tpName)));
            }
            sb.append(">");
        }

        sb.append("(");
        boolean first = true;
        for (ParamSymbol p : cls.getParameters().values()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(p.getName()).append(": ");
            String t = p.getType() != null ? renderType(p.getType()) : "?";
            sb.append(t);
        }
        sb.append(")");
        return sb.toString();
    }

    private String renderTypeParamDecl(TypeParamSymbol tp) {
        if (tp == null) return null;
        if (tp.getBounds().isEmpty()) return tp.getName();

        StringBuilder sb = new StringBuilder(tp.getName()).append(": ");
        boolean first = true;
        for (BoundSymbol b : tp.getBounds().values()) {
            Symbol bt = b.getType();
            if (bt == null) continue;
            if (!first) sb.append(" & ");
            first = false;
            sb.append(renderType(bt));
        }
        return first ? tp.getName() : sb.toString();
    }

    private static String renderTypedForHover(Typed typed) {
        if (typed instanceof RefTyped ref) {
            return ref.name != null ? ref.name.source : "unknown";
        }
        if (typed instanceof ParameterizedRefTyped pref) {
            StringBuilder sb = new StringBuilder(pref.name != null ? pref.name.source : "?");
            sb.append("<");
            if (pref.typeArguments != null) {
                boolean first = true;
                for (Typed arg : pref.typeArguments) {
                    if (!first) sb.append(", ");
                    first = false;
                    sb.append(renderTypedForHover(arg));
                }
            }
            sb.append(">");
            return sb.toString();
        }
        return "unknown";
    }

    private static String typedBaseName(Typed typed) {
        if (typed instanceof RefTyped ref) return ref.name != null ? ref.name.source : null;
        if (typed instanceof ParameterizedRefTyped pref) return pref.name != null ? pref.name.source : null;
        return null;
    }

    /** Mapeia a constante de {@link LiteralExpr} para o nome do tipo. */
    private static String literalTypeName(int literalType) {
        return switch (literalType) {
            case LiteralExpr.NULL -> "Null";
            case LiteralExpr.BOOL -> "Bool";
            case LiteralExpr.INT -> "Int";
            case LiteralExpr.FLOAT -> "Float";
            case LiteralExpr.STRING -> "String";
            default -> "unknown";
        };
    }

    // ==================== DIAGNOSTICS ====================

    private org.eclipse.lsp4j.Diagnostic toLspDiagnostic(com.lang.util.Diagnostic d) {
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
        return new Range(new Position(startLine, startCol), new Position(endLine, endCol));
    }

    private Range safeRange(com.lang.util.Position p) {
        if (p == null) return null;
        int first = p.getFirstLineCount();
        int last  = p.getLastLineCount();
        if (first <= 0 || last <= 0) return null;
        if (p.getStart() < 0 || p.getEnd() < 0) return null;
        if (p.getEnd() < p.getStart()) return null;

        int startLine = first - 1;
        int endLine   = last - 1;
        int startCol  = Math.max(0, p.getColumnStart());
        int endCol    = Math.max(0, p.getColumnEnd());

        return new Range(new Position(startLine, startCol), new Position(endLine, endCol));
    }

    private DiagnosticSeverity toSeverity(int sev) {
        return switch (sev) {
            case com.lang.util.Diagnostic.ERROR -> DiagnosticSeverity.Error;
            case com.lang.util.Diagnostic.WARNING -> DiagnosticSeverity.Warning;
            case com.lang.util.Diagnostic.INFO -> DiagnosticSeverity.Information;
            default -> DiagnosticSeverity.Hint;
        };
    }

    // ==================== HOVER ====================

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        String uri = params.getTextDocument().getUri();
        int line = params.getPosition().getLine();
        int character = params.getPosition().getCharacter();

        CommentEntry ce = lookupComment(uri, line, character);
        if (ce != null) {
            return CompletableFuture.completedFuture(
                    buildHover(null, null, ce.text(), null, ce.range()));
        }

        RefEntry re = lookupRef(uri, line, character);
        if (re != null) {
            String symbolDoc = lookupDocForSymbol(uri, re.symbol());
            String typeDoc = (re.signature() == null)
                    ? lookupDocForTypeName(uri, baseTypeName(re.typeName()))
                    : null;
            Range r = new Range(new Position(line, re.startCharacter()),
                                new Position(line, re.endCharacter()));
            return CompletableFuture.completedFuture(
                    buildHover(re.typeName(), re.signature(), symbolDoc, typeDoc, r));
        }

        LiteralEntry le = lookupLiteral(uri, line, character);
        if (le != null) {
            String typeName = literalTypeName(le.literalType());
            String typeDoc = lookupDocForTypeName(uri, typeName);
            Range r = new Range(new Position(line, le.startCharacter()),
                                new Position(line, le.endCharacter()));
            return CompletableFuture.completedFuture(
                    buildHover(typeName, null, null, typeDoc, r));
        }

        TypeRefEntry te = lookupTypeRef(uri, line, character);
        if (te != null) {
            String typeName = renderTypedForHover(te.typed());
            String typeDoc = lookupDocForTypeName(uri, typedBaseName(te.typed()));
            Range r = new Range(new Position(line, te.startCharacter()),
                                new Position(line, te.endCharacter()));
            return CompletableFuture.completedFuture(
                    buildHover(typeName, null, null, typeDoc, r));
        }

        Map<Integer, String> byTargetLine = lineDocs.get(uri);
        if (byTargetLine != null) {
            String doc = byTargetLine.get(line);
            if (doc != null) {
                return CompletableFuture.completedFuture(
                        buildHover(null, null, doc, null, null));
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    private CommentEntry lookupComment(String uri, int line, int character) {
        Map<Integer, CommentEntry> byCommentLine = commentIndex.get(uri);
        if (byCommentLine == null) return null;
        CommentEntry entry = byCommentLine.get(line);
        if (entry != null && cursorInRange(entry.range(), line, character)) return entry;
        return null;
    }

    private RefEntry lookupRef(String uri, int line, int character) {
        Map<Integer, List<RefEntry>> byRefLine = refIndex.get(uri);
        if (byRefLine == null) return null;
        List<RefEntry> refs = byRefLine.get(line);
        if (refs == null) return null;
        for (RefEntry ref : refs) {
            if (character >= ref.startCharacter() && character <= ref.endCharacter()) return ref;
        }
        return null;
    }

    private LiteralEntry lookupLiteral(String uri, int line, int character) {
        Map<Integer, List<LiteralEntry>> byLine = literalIndex.get(uri);
        if (byLine == null) return null;
        List<LiteralEntry> list = byLine.get(line);
        if (list == null) return null;
        for (LiteralEntry le : list) {
            if (character >= le.startCharacter() && character <= le.endCharacter()) return le;
        }
        return null;
    }

    private TypeRefEntry lookupTypeRef(String uri, int line, int character) {
        Map<Integer, List<TypeRefEntry>> byLine = typeRefIndex.get(uri);
        if (byLine == null) return null;
        List<TypeRefEntry> refs = byLine.get(line);
        if (refs == null) return null;
        for (TypeRefEntry te : refs) {
            if (character >= te.startCharacter() && character <= te.endCharacter()) return te;
        }
        return null;
    }

    private String lookupDocForSymbol(String uri, Symbol sym) {
        if (sym == null || sym.node == null) return null;
        Map<Node, String> local = docByNode.get(uri);
        if (local != null) {
            String doc = local.get(sym.node);
            if (doc != null) return doc;
        }
        return stubDocsByNode().get(sym.node);
    }

    private String lookupDocForTypeName(String uri, String name) {
        if (name == null) return null;

        SymbolTable table = tables.get(uri);
        if (table != null) {
            ClassSymbol cls = table.get(name);
            if (cls != null && cls.node != null) {
                Map<Node, String> local = docByNode.get(uri);
                if (local != null) {
                    String doc = local.get(cls.node);
                    if (doc != null) return doc;
                }
            }
        }

        ClassSymbol stubCls = stubTable.get(name);
        if (stubCls != null && stubCls.node != null) {
            return stubDocsByNode().get(stubCls.node);
        }
        return null;
    }

    private Map<Node, String> stubDocsByNode() {
        Map<Node, String> cached = cachedStubDocs;
        if (cached != null) return cached;

        synchronized (this) {
            if (cachedStubDocs != null) return cachedStubDocs;

            Map<Node, String> docs = new IdentityHashMap<>();
            Map<Integer, String> textByEndLine = new HashMap<>();

            List<Comment> comments = stubUnit.getComments();
            if (comments != null) {
                for (Comment c : comments) {
                    if (c == null || c.text == null || c.position == null) continue;
                    String clean = cleanComment(c.text);
                    if (clean.isEmpty()) continue;
                    int endLine0 = Math.max(0, c.position.getLastLineCount() - 1);
                    textByEndLine.put(endLine0, clean);
                }
            }

            for (Decl decl : collectDeclarations(stubUnit)) {
                if (decl == null || decl.symbol == null) continue;
                Symbol s = decl.symbol;
                if (s.node == null || s.node.getPosition() == null) continue;
                int firstLine1 = s.node.getPosition().getFirstLineCount();
                if (firstLine1 <= 0) continue;
                int targetLine0 = firstLine1 - 1;
                if (targetLine0 <= 0) continue;
                String doc = textByEndLine.get(targetLine0 - 1);
                if (doc != null) docs.put(s.node, doc);
            }

            cachedStubDocs = docs;
            return docs;
        }
    }

    private static boolean cursorInRange(Range range, int line, int character) {
        Position start = range.getStart();
        Position end = range.getEnd();
        if (line < start.getLine() || line > end.getLine()) return false;
        if (line == start.getLine() && character < start.getCharacter()) return false;
        if (line == end.getLine() && character > end.getCharacter()) return false;
        return true;
    }

    private static Hover buildHover(String typeName,
                                    String signature,
                                    String symbolDoc,
                                    String typeDoc,
                                    Range range) {
        StringBuilder sb = new StringBuilder();

        if (signature != null && !signature.isEmpty()) {
            sb.append("```").append(LANG_ID).append("\n")
              .append(signature).append("\n```");
        } else if (typeName != null && !typeName.isEmpty()) {
            sb.append("**`").append(typeName).append("`**");
        }

        if (symbolDoc != null && !symbolDoc.isEmpty()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(symbolDoc);
        }

        if (typeDoc != null && !typeDoc.isEmpty()) {
            if (sb.length() > 0) sb.append("\n\n---\n\n");
            sb.append("*").append(typeDoc).append("*");
        }

        if (sb.length() == 0) return null;

        MarkupContent markup = new MarkupContent(MarkupKind.MARKDOWN, sb.toString());
        Hover hover = new Hover(markup);
        if (range != null) hover.setRange(range);
        return hover;
    }

    // ==================== INLAY HINTS ====================

    @Override
    public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
        CompilationUnit unit = cache.get(params.getTextDocument().getUri());
        if (unit == null) return CompletableFuture.completedFuture(List.of());

        Range range = params.getRange();
        int startLine = range.getStart().getLine();
        int endLine = range.getEnd().getLine();

        List<InlayHint> out = new ArrayList<>();

        for (com.lang.util.Diagnostic d : unit.getDiagnostics(TAG)) {
            if (!d.isHint()) continue;
            com.lang.util.Position p = d.getPosition();
            if (p == null || p.getFirstLineCount() <= 0) continue;

            int line = p.getFirstLineCount() - 1;
            if (line < startLine || line > endLine) continue;

            int col = p.getColumnStart();
            String label = d.getMessage();

            boolean isDefaultValue = label.startsWith(HINT_DEFAULT_MARKER);
            if (isDefaultValue) {
                label = label.substring(HINT_DEFAULT_MARKER.length());
            }

            InlayHint hint = new InlayHint(
                    new Position(line, col),
                    Either.forLeft(label));
            if (isDefaultValue) {
                hint.setPaddingLeft(true);
                hint.setPaddingRight(false);
            } else {
                hint.setPaddingRight(true);
            }
            out.add(hint);
        }

        out.sort(Comparator
                .comparingInt((InlayHint h) -> h.getPosition().getLine())
                .thenComparingInt(h -> h.getPosition().getCharacter()));

        return CompletableFuture.completedFuture(out);
    }

    // ==================== RENAME ====================

    @Override
    public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> prepareRename(PrepareRenameParams params) {
        String uri = params.getTextDocument().getUri();
        int line = params.getPosition().getLine();
        int character = params.getPosition().getCharacter();

        RefEntry re = lookupRef(uri, line, character);
        if (re != null) {
            Range r = new Range(new Position(line, re.startCharacter()),
                                new Position(line, re.endCharacter()));
            return CompletableFuture.completedFuture(Either3.forFirst(r));
        }

        DeclNameEntry de = lookupDeclName(uri, line, character);
        if (de != null) {
            Range r = new Range(new Position(line, de.startCharacter()),
                                new Position(line, de.endCharacter()));
            return CompletableFuture.completedFuture(Either3.forFirst(r));
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        String uri = params.getTextDocument().getUri();
        int line = params.getPosition().getLine();
        int character = params.getPosition().getCharacter();
        String newName = params.getNewName();

        if (newName == null || newName.isEmpty() || !isValidIdentifier(newName)) {
            return CompletableFuture.completedFuture(null);
        }

        Symbol target = null;
        RefEntry re = lookupRef(uri, line, character);
        if (re != null) {
            target = re.symbol();
        } else {
            DeclNameEntry de = lookupDeclName(uri, line, character);
            if (de != null) {
                target = de.symbol();
            }
        }

        if (target == null) {
            return CompletableFuture.completedFuture(null);
        }

        List<TextEdit> edits = new ArrayList<>();

        Map<Integer, List<RefEntry>> byRefLine = refIndex.get(uri);
        if (byRefLine != null) {
            for (var e : byRefLine.entrySet()) {
                int line0 = e.getKey();
                for (RefEntry ref : e.getValue()) {
                    if (ref.symbol() == target) {
                        edits.add(new TextEdit(
                                new Range(new Position(line0, ref.startCharacter()),
                                          new Position(line0, ref.endCharacter())),
                                newName));
                    }
                }
            }
        }

        Map<Integer, List<DeclNameEntry>> byDeclLine = declNameIndex.get(uri);
        if (byDeclLine != null) {
            for (var e : byDeclLine.entrySet()) {
                int line0 = e.getKey();
                for (DeclNameEntry de : e.getValue()) {
                    if (de.symbol() == target) {
                        edits.add(new TextEdit(
                                new Range(new Position(line0, de.startCharacter()),
                                          new Position(line0, de.endCharacter())),
                                newName));
                    }
                }
            }
        }

        if (edits.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        WorkspaceEdit we = new WorkspaceEdit();
        we.setChanges(Map.of(uri, edits));
        return CompletableFuture.completedFuture(we);
    }

    private DeclNameEntry lookupDeclName(String uri, int line, int character) {
        Map<Integer, List<DeclNameEntry>> byLine = declNameIndex.get(uri);
        if (byLine == null) return null;
        List<DeclNameEntry> list = byLine.get(line);
        if (list == null) return null;
        for (DeclNameEntry de : list) {
            if (character >= de.startCharacter() && character <= de.endCharacter()) return de;
        }
        return null;
    }

    private static boolean isValidIdentifier(String name) {
        if (name == null || name.isEmpty()) return false;
        int first = name.codePointAt(0);
        if (!Codepoint.isIdentifierStart(first)) return false;
        for (int i = Character.charCount(first); i < name.length(); ) {
            int cp = name.codePointAt(i);
            if (!Codepoint.isIdentifier(cp)) return false;
            i += Character.charCount(cp);
        }
        return true;
    }

    // ==================== SELECTION RANGE ====================

    @Override
    public CompletableFuture<List<SelectionRange>> selectionRange(SelectionRangeParams params) {
        String uri = params.getTextDocument().getUri();
        CompilationUnit unit = cache.get(uri);
        if (unit == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<SelectionRange> out = new ArrayList<>();
        for (Position pos : params.getPositions()) {
            SelectionRange chain = buildSelectionChain(unit, pos);
            if (chain != null) out.add(chain);
        }
        return CompletableFuture.completedFuture(out);
    }

    private SelectionRange buildSelectionChain(CompilationUnit unit, Position pos) {
        List<Range> chain = new ArrayList<>();
        int line = pos.getLine();
        int character = pos.getCharacter();

        for (ClassDecl cls : unit.getClasses()) {
            if (cls == null) continue;
            collectSelectionRanges(cls, line, character, chain);
        }

        if (chain.isEmpty()) return null;

        SelectionRange result = null;
        for (int i = 0; i < chain.size(); i++) {
            result = new SelectionRange(chain.get(i), result);
        }
        return result;
    }

    private void collectSelectionRanges(Node node, int line, int character, List<Range> chain) {
        if (node == null || node.getPosition() == null) return;

        Range range = safeRange(node.getPosition());
        if (range == null) return;
        if (!cursorInRange(range, line, character)) return;

        chain.add(range);

        if (node instanceof ClassDecl cls) {
            if (cls.name != null) collectSelectionRanges(cls.name, line, character, chain);
            if (cls.typeParameters != null) for (var tp : cls.typeParameters) collectSelectionRanges(tp, line, character, chain);
            if (cls.parameters != null) for (var p : cls.parameters) collectSelectionRanges(p, line, character, chain);
            if (cls.supers != null) for (var s : cls.supers) collectSelectionRanges(s, line, character, chain);
            if (cls.members != null) for (var m : cls.members) collectSelectionRanges(m, line, character, chain);
        } else if (node instanceof FunDecl fun) {
            if (fun.name != null) collectSelectionRanges(fun.name, line, character, chain);
            if (fun.type != null) collectSelectionRanges(fun.type, line, character, chain);
            if (fun.typeParameters != null) for (var tp : fun.typeParameters) collectSelectionRanges(tp, line, character, chain);
            if (fun.parameters != null) for (var p : fun.parameters) collectSelectionRanges(p, line, character, chain);
            if (fun.body != null) collectSelectionRanges(fun.body, line, character, chain);
        } else if (node instanceof VarDecl var) {
            if (var.name != null) collectSelectionRanges(var.name, line, character, chain);
            if (var.type != null) collectSelectionRanges(var.type, line, character, chain);
            if (var.value != null) collectSelectionRanges(var.value, line, character, chain);
        } else if (node instanceof ParamDecl p) {
            if (p.name != null) collectSelectionRanges(p.name, line, character, chain);
            if (p.type != null) collectSelectionRanges(p.type, line, character, chain);
            if (p.init != null) collectSelectionRanges(p.init, line, character, chain);
        } else if (node instanceof TypeParamDecl tp) {
            if (tp.name != null) collectSelectionRanges(tp.name, line, character, chain);
            if (tp.bounds != null) for (var b : tp.bounds) collectSelectionRanges(b, line, character, chain);
        } else if (node instanceof SuperDecl s) {
            if (s.type != null) collectSelectionRanges(s.type, line, character, chain);
            if (s.arguments != null) for (var e : s.arguments) collectSelectionRanges(e, line, character, chain);
        } else if (node instanceof BoundDecl b) {
            if (b.type != null) collectSelectionRanges(b.type, line, character, chain);
        } else if (node instanceof BlockStmt b) {
            if (b.statements != null) for (var st : b.statements) collectSelectionRanges(st, line, character, chain);
        } else if (node instanceof LetStmt l) {
            if (l.name != null) collectSelectionRanges(l.name, line, character, chain);
            if (l.type != null) collectSelectionRanges(l.type, line, character, chain);
            if (l.value != null) collectSelectionRanges(l.value, line, character, chain);
        } else if (node instanceof ExprStmt e) {
            if (e.expression != null) collectSelectionRanges(e.expression, line, character, chain);
        } else if (node instanceof ReturnStmt r) {
            if (r.value != null) collectSelectionRanges(r.value, line, character, chain);
        } else if (node instanceof IfStmt i) {
            if (i.condition != null) collectSelectionRanges(i.condition, line, character, chain);
            if (i.thenBranch != null) collectSelectionRanges(i.thenBranch, line, character, chain);
            if (i.elseBranch != null) collectSelectionRanges(i.elseBranch, line, character, chain);
        } else if (node instanceof WhileStmt w) {
            if (w.condition != null) collectSelectionRanges(w.condition, line, character, chain);
            if (w.body != null) collectSelectionRanges(w.body, line, character, chain);
        } else if (node instanceof RefExpr ref) {
            if (ref.name != null) collectSelectionRanges(ref.name, line, character, chain);
        } else if (node instanceof MemberAccessExpr m) {
            if (m.object != null) collectSelectionRanges(m.object, line, character, chain);
            if (m.name != null) collectSelectionRanges(m.name, line, character, chain);
        } else if (node instanceof CallExpr c) {
            if (c.callee != null) collectSelectionRanges(c.callee, line, character, chain);
            if (c.typeArguments != null) for (var t : c.typeArguments) collectSelectionRanges(t, line, character, chain);
            if (c.arguments != null) for (var a : c.arguments) collectSelectionRanges(a, line, character, chain);
        } else if (node instanceof BinaryExpr b) {
            if (b.left != null) collectSelectionRanges(b.left, line, character, chain);
            if (b.right != null) collectSelectionRanges(b.right, line, character, chain);
        } else if (node instanceof UnaryExpr u) {
            if (u.operand != null) collectSelectionRanges(u.operand, line, character, chain);
        } else if (node instanceof AssignExpr a) {
            if (a.target != null) collectSelectionRanges(a.target, line, character, chain);
            if (a.value != null) collectSelectionRanges(a.value, line, character, chain);
        } else if (node instanceof ParenthesisExpr p) {
            if (p.inner != null) collectSelectionRanges(p.inner, line, character, chain);
        } else if (node instanceof Typed t) {
            if (t instanceof RefTyped rt) {
                if (rt.name != null) collectSelectionRanges(rt.name, line, character, chain);
            } else if (t instanceof ParameterizedRefTyped prt) {
                if (prt.name != null) collectSelectionRanges(prt.name, line, character, chain);
                if (prt.typeArguments != null) for (var arg : prt.typeArguments) collectSelectionRanges(arg, line, character, chain);
            }
        }
    }

    // ==================== DOCUMENT SYMBOL ====================

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
            DocumentSymbolParams params) {
        String uri = params.getTextDocument().getUri();
        CompilationUnit unit = cache.get(uri);
        if (unit == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<Either<SymbolInformation, DocumentSymbol>> out = new ArrayList<>();
        for (ClassDecl cls : unit.getClasses()) {
            DocumentSymbol ds = buildClassSymbol(cls);
            if (ds != null) out.add(Either.forRight(ds));
        }
        return CompletableFuture.completedFuture(out);
    }

    private DocumentSymbol buildClassSymbol(ClassDecl cls) {
        if (cls == null || cls.name == null) return null;

        Range fullRange = safeRange(cls.getPosition());
        Range nameRange = safeRange(cls.name.getPosition());
        if (fullRange == null || nameRange == null) return null;

        List<DocumentSymbol> children = new ArrayList<>();

        if (cls.typeParameters != null) {
            for (TypeParamDecl tp : cls.typeParameters) {
                DocumentSymbol ds = buildTypeParamSymbol(tp);
                if (ds != null) children.add(ds);
            }
        }
        if (cls.members != null) {
            for (var m : cls.members) {
                if (m instanceof FunDecl fun) {
                    DocumentSymbol ds = buildFunSymbol(fun);
                    if (ds != null) children.add(ds);
                } else if (m instanceof VarDecl var) {
                    DocumentSymbol ds = buildVarSymbol(var);
                    if (ds != null) children.add(ds);
                }
            }
        }

        DocumentSymbol ds = new DocumentSymbol(
                cls.name.source,
                SymbolKind.Class,
                fullRange,
                nameRange);
        ds.setChildren(children);
        if (cls.symbol != null && cls.symbol.isClass()) {
            ds.setDetail(renderClassSignature(cls.symbol.asClass()));
        }
        return ds;
    }

    private DocumentSymbol buildFunSymbol(FunDecl fun) {
        if (fun == null || fun.name == null) return null;

        Range fullRange = safeRange(fun.getPosition());
        Range nameRange = safeRange(fun.name.getPosition());
        if (fullRange == null || nameRange == null) return null;

        List<DocumentSymbol> children = new ArrayList<>();

        if (fun.typeParameters != null) {
            for (TypeParamDecl tp : fun.typeParameters) {
                DocumentSymbol ds = buildTypeParamSymbol(tp);
                if (ds != null) children.add(ds);
            }
        }

        String detail = null;
        if (fun.symbol != null && fun.symbol.isFun()) {
            detail = renderFunctionSignature(fun.symbol.asFun());
        }

        DocumentSymbol ds = new DocumentSymbol(
                fun.name.source,
                SymbolKind.Method,
                fullRange,
                nameRange);
        ds.setChildren(children);
        if (detail != null) ds.setDetail(detail);
        return ds;
    }

    private DocumentSymbol buildVarSymbol(VarDecl var) {
        if (var == null || var.name == null) return null;

        Range fullRange = safeRange(var.getPosition());
        Range nameRange = safeRange(var.name.getPosition());
        if (fullRange == null || nameRange == null) return null;

        DocumentSymbol ds = new DocumentSymbol(
                var.name.source,
                SymbolKind.Field,
                fullRange,
                nameRange);
        if (var.symbol != null && var.symbol.isVar() && var.symbol.asVar().getType() != null) {
            ds.setDetail(renderTypeWithBounds(var.symbol.asVar().getType()));
        }
        return ds;
    }

    private DocumentSymbol buildTypeParamSymbol(TypeParamDecl tp) {
        if (tp == null || tp.name == null) return null;

        Range fullRange = safeRange(tp.getPosition());
        Range nameRange = safeRange(tp.name.getPosition());
        if (fullRange == null || nameRange == null) return null;

        DocumentSymbol ds = new DocumentSymbol(
                tp.name.source,
                SymbolKind.TypeParameter,
                fullRange,
                nameRange);

        if (tp.symbol != null && tp.symbol.isTypeParam()) {
            ds.setDetail(renderTypeParamDecl(tp.symbol.asTypeParam()));
        }
        return ds;
    }

    // ==================== DEFINITION ====================

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
            DefinitionParams params) {
        String uri = params.getTextDocument().getUri();
        int line = params.getPosition().getLine();
        int character = params.getPosition().getCharacter();

        Location loc = null;

        RefEntry re = lookupRef(uri, line, character);
        if (re != null) {
            loc = locationForSymbol(uri, re.symbol());
        }

        if (loc == null) {
            DeclNameEntry de = lookupDeclName(uri, line, character);
            if (de != null) {
                loc = locationForSymbol(uri, de.symbol());
            }
        }

        if (loc == null) {
            TypeRefEntry te = lookupTypeRef(uri, line, character);
            if (te != null) {
                loc = locationForTypeName(uri, typedBaseName(te.typed()));
            }
        }

        if (loc == null) {
            return CompletableFuture.completedFuture(Either.forLeft(List.of()));
        }
        return CompletableFuture.completedFuture(Either.forLeft(List.of(loc)));
    }

    private Location locationForSymbol(String uri, Symbol sym) {
        if (sym == null || sym.node == null) return null;

        Node node = sym.node;
        Identifier name = nameIdentifierOf(node);
        if (name == null || name.position == null) return null;

        Range range = safeRange(name.position);
        if (range == null) return null;
        return new Location(uri, range);
    }

    private static Identifier nameIdentifierOf(Node node) {
        if (node instanceof ClassDecl c) return c.name;
        if (node instanceof FunDecl f) return f.name;
        if (node instanceof VarDecl v) return v.name;
        if (node instanceof ParamDecl p) return p.name;
        if (node instanceof TypeParamDecl t) return t.name;
        return null;
    }

    private Location locationForTypeName(String uri, String name) {
        if (name == null) return null;

        SymbolTable table = tables.get(uri);
        if (table != null) {
            ClassSymbol cls = table.get(name);
            if (cls != null && cls.node instanceof ClassDecl cd && cd.name != null) {
                Range range = safeRange(cd.name.position);
                if (range == null) return null;
                return new Location(uri, range);
            }
        }
        return null;
    }
}