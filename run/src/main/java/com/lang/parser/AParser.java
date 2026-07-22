package com.lang.parser;

import static com.lang.parser.ParsingErrorCode.*;
import static com.lang.token.Type.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.lang.ast.*;
import com.lang.token.*;
import com.lang.unit.*;
import com.lang.util.*;

public class AParser implements StmtVisitor<Void> {

    private final Stack<Stmt> stack = new Stack<>();

    private final TokenStream stream;
    private final CompilationUnit unit;

    public AParser(TokenStream stream, CompilationUnit unit) {
        this.stream = stream;
        this.unit = unit != null ? unit : new CompilationUnit();

        if (this.stream == null) {
            throw new IllegalArgumentException("TokenStream cannot be null");
        }
        this.stream.setHandler((token) -> {
            if (token.isComment()) {
                this.unit.addComment(new Comment(token.lexeme, token.position));
                return false;
            }
            return true;
        });

    }

    private Position pos(Positioned p) {
        return p.getPosition();
    }

    private Position between(Positioned start, Positioned end) {
        return Position.between(start.getPosition(), end.getPosition());
    }

    public CompilationUnit parse() {
        program();
        return unit;
    }

    private void program() {
        while (!stream.isAtEnd() && !stream.check(EOF)) {
            if (stream.check("class")) {

            } else {
                throw unit.error(
                        TAG,
                        EXPECTED_TOKEN.format("class", stream.peek().type),
                        stream.peek());
            }
        }
    }

    public void run() {
        while (!stack.isEmpty()) {
            Stmt stmt = stack.peek();
            stmt.accept(this);
        }
    }

    private void semicolon() {
        if (!stream.match(SEMICOLON)) {
            unit.addError(
                    unit.error(
                            TAG,
                            EXPECTED_TOKEN.format(";", stream.peek().type),
                            stream.peek()));
        }
    }

    private Pair<Typed, List<Typed>> inheritance() {
        Typed superclass = null;
        List<Typed> supertraits = new ArrayList<>();

        if (stream.match(COLON)) {
            superclass = type();
        }

        if (stream.match(BAR)) {
            do {
                supertraits.add(type());
            } while (stream.match(AMP));
        }

        return Pair.of(superclass, supertraits);
    }

    private List<TypeParamDeclStmt> typeParameters() {
        stream.expect(LANGLE);

        List<TypeParamDeclStmt> params = new ArrayList<>();

        do {
            Token nameToken = stream.expect(IDENTIFIER);

            Pair<Typed, List<Typed>> inherit = inheritance();

            Typed superclass = inherit.first;
            List<Typed> supertraits = inherit.second;

            Positioned end = !supertraits.isEmpty()
                    ? supertraits.get(supertraits.size() - 1)
                    : superclass != null
                            ? superclass
                            : nameToken;

            params.add(
                    new TypeParamDeclStmt(
                            nameToken.lexeme,
                            superclass,
                            supertraits,
                            between(nameToken, end)));
        } while (stream.match(COMMA));

        stream.expect(RANGLE);
        return params;
    }

    private List<Typed> typeArguments() {
        stream.expect(LANGLE);

        List<Typed> args = new ArrayList<>();

        if (stream.match(RANGLE))
            return args;

        do {
            args.add(type());
        } while (stream.match(COMMA));

        stream.expect(RANGLE);
        return args;
    }

    private Typed type() {
        Token nameToken = stream.expect(IDENTIFIER);

        if (stream.check(LANGLE)) {
            List<Typed> types = typeArguments();
            return new ParameterizedRefTyped(
                    nameToken.lexeme,
                    types,
                    between(nameToken, stream.previous()));
        }

        return new RefTyped(nameToken.lexeme, pos(nameToken));
    }

    @Override
    public Void visitBlockStmt(BlockStmt stmt) {
        if (stmt.statements == null) {
            stmt.statements = new ArrayList<>();
            stream.expect(LBRACE);
            if (stream.check("let"))
                stack.push(new LetDeclStmt());
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(BreakStmt stmt) {
        Token token = stream.expect("break");
        stmt.position = token.position;
        stack.pop();
        return null;
    }

    @Override
    public Void visitClassDeclStmt(ClassDeclStmt stmt) {
        if (stmt.name == null) {
            stream.expect("class");
            stmt.name = stream.expect(IDENTIFIER).lexeme;
            return null;
        }

        if (stream.check(LANGLE))
            stmt.typeParameters = typeParameters();

        Pair<Typed, List<Typed>> inherit = inheritance();

        

        return null;
    }

    @Override
    public Void visitContinueStmt(ContinueStmt stmt) {
        Token token = stream.expect("continue");
        stmt.position = token.position;
        stack.pop();
        return null;
    }

    @Override
    public Void visitExprStmt(ExprStmt stmt) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitFunDeclStmt(FunDeclStmt stmt) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitIfStmt(IfStmt stmt) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitLetDeclStmt(LetDeclStmt stmt) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitParamDeclStmt(ParamDeclStmt stmt) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitReturnStmt(ReturnStmt stmt) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitTypeParamDeclStmt(TypeParamDeclStmt stmt) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitVarDeclStmt(VarDeclStmt stmt) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void visitWhileStmt(WhileStmt stmt) {
        // TODO Auto-generated method stub
        return null;
    }
}
