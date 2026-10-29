package com.lang.unit;

import com.lang.ast.ClassDecl;
import com.lang.ast.Comment;
import com.lang.ast.FunDecl;
import com.lang.ast.OperatorDecl;
import com.lang.ast.VarDecl;
import com.lang.util.Diagnostic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompilationUnit {
    private final String filePath;
    private final String fileName;

    private final List<Comment> comments = new ArrayList<>();

    private final List<ClassDecl> classes = new ArrayList<>();
    private final List<VarDecl> vars = new ArrayList<>();
    private final List<FunDecl> funs = new ArrayList<>();
    private final List<OperatorDecl> operators = new ArrayList<>();

    private final Map<String, List<Diagnostic>> diagnostics = new HashMap<>();

    public CompilationUnit(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void addComment(Comment comment) {
        if (comment != null)
            comments.add(comment);
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void addClass(ClassDecl cls) {
        if (cls != null)
            classes.add(cls);
    }

    public List<ClassDecl> getClasses() {
        return classes;
    }

    public void addVar(VarDecl var) {
        if (var != null)
            vars.add(var);
    }

    public List<VarDecl> getVars() {
        return vars;
    }

    public void addFunction(FunDecl fn) {
        if (fn != null)
            funs.add(fn);
    }

    public List<FunDecl> getFuns() {
        return funs;
    }

    public void addOperator(OperatorDecl op) {
        if (op != null)
            operators.add(op);
    }

    public List<OperatorDecl> getOperators() {
        return operators;
    }

    public void addDiagnostic(String tag, Diagnostic diagnostic) {
        if (diagnostic == null)
            return;
        diagnostics.computeIfAbsent(tag, k -> new ArrayList<>()).add(diagnostic);
    }

    public List<Diagnostic> getDiagnostics(String tag) {
        return diagnostics.getOrDefault(tag, new ArrayList<>());
    }

    public Map<String, List<Diagnostic>> getDiagnostics() {
        return diagnostics;
    }

    public void printReport() {
        printReport(Diagnostic.INFO);
    }

    public void printReport(int level) {
        if (level <= 0)
            return;
        boolean any = diagnostics.values().stream()
                .flatMap(List::stream)
                .anyMatch(d -> d.getSeverity() <= level);
        if (!any) {
            System.out.println("OK: " + fileName);
            return;
        }
        System.out.println("\n=== COMPILATION REPORT: " + fileName + " ===");
        for (Map.Entry<String, List<Diagnostic>> entry : diagnostics.entrySet()) {
            List<Diagnostic> list = entry.getValue().stream()
                    .filter(d -> d.getSeverity() <= level)
                    .toList();
            if (list.isEmpty())
                continue;
            System.out.printf("  [%s] %d diagnostic(s):%n", entry.getKey(), list.size());
            for (Diagnostic d : list) {
                System.out.printf("      %s%n", d.format());
            }
        }
    }
}