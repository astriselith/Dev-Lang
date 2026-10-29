package com.lang.parser;

import static com.lang.parser.ParsingErrorCode.*;
import static com.lang.token.Token.*;

import com.lang.ast.*;
import com.lang.token.*;
import com.lang.unit.CompilationUnit;
import com.lang.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Parser {
    private static final Set<String> DEFAULT_STMT_RECOVERY = Set.of(
            "let", "if", "while", "break", "continue", "return");
    private static final Set<String> DEFAULT_MEMBER_RECOVERY = Set.of(
            "fun", "var", "operator", "}");
    private static final Set<String> DEFAULT_TOPLEVEL_RECOVERY = Set.of(
            "class", "fun", "var", "operator");

    private static final int[] DEFAULT_OPERATOR_TYPE = {
            PLUS, MINUS, STAR, SLASH, PERCENT,
            EQUALS, BANG, LANGLE, RANGLE, AMP, BAR, QUESTION, TILDE
    };

    private static final int[] DEFAULT_PRIMARY_TYPE = {
            NULL, BOOL, INT, FLOAT, STRING, IDENTIFIER, LPAREN
    };

    private final TokenStream stream;
    private final CompilationUnit unit;

    public Parser(TokenStream stream, CompilationUnit unit) {
        this.stream = stream;
        this.unit = unit;
        if (this.stream == null)
            throw new IllegalArgumentException("TokenStream cannot be null");
        this.stream.setFilter(t -> {
            if (t.is(COMMENT)) {
                unit.addComment(new Comment(t.lexeme, t.position));
                return false;
            }
            return true;
        });
    }

    public CompilationUnit parse() {
        program();
        return unit;
    }

    private IllegalStateException error(String message, Positioned positioned) {
        Position position = positioned != null ? positioned.getPosition() : null;
        unit.addDiagnostic(TAG, Diagnostic.error(position, message));
        return new IllegalStateException(message);
    }

    private Token expect(int type) {
        Token t = stream.peek();
        if (t == null || !t.is(type))
            throw error(EXPECTED_TOKEN.format(Token.typeName(type),
                    Token.typeName(t != null ? t.type : Token.UNDEFINED)), t);
        return stream.advance();
    }

    private Token expect(String lexeme) {
        Token t = stream.peek();
        if (t == null || !t.lexemeEquals(lexeme))
            throw error(EXPECTED_TOKEN.format("\"" + lexeme + "\"",
                    Token.typeName(t != null ? t.type : Token.UNDEFINED)), t);
        return stream.advance();
    }

    private void synchronizeAfter(Set<String> recoveryPoints) {
        while (!stream.isAtEnd() && !stream.check(EOF)) {
            if (stream.match(SEMICOLON))
                return;
            for (String lex : recoveryPoints)
                if (stream.check(lex))
                    return;
            stream.advance();
        }
    }

    private Position pos(Positioned p) {
        return p.getPosition();
    }

    private Position between(Positioned a, Positioned b) {
        return Position.between(a.getPosition(), b.getPosition());
    }

    private Position span(Token start, Positioned end) {
        return between(start, end != null ? end : stream.previous());
    }

    private void semicolon() {
        if (!stream.match(SEMICOLON)) {
            Token t = stream.peek();
            unit.addDiagnostic(TAG, Diagnostic.error(t.position,
                    EXPECTED_TOKEN.format(";", Token.typeName(t.type))));
        }
    }

    // ==================== PROGRAM ====================

    private void program() {
        while (!stream.isAtEnd() && !stream.check(EOF)) {
            try {
                if (stream.check("class"))
                    unit.addClass(classDecl());
                else if (stream.check("fun")) {
                    unit.addFunction(funDecl());
                    semicolon();
                } else if (stream.check("var")) {
                    unit.addVar(varDecl());
                    semicolon();
                } else if (stream.check("operator")) {
                    unit.addOperator(operatorDecl());
                    semicolon();
                } else
                    throw error(EXPECTED_TOKEN.format("class/fun/var/operator",
                            Token.typeName(stream.peek().type)), stream.peek());
            } catch (IllegalStateException e) {
                synchronizeAfter(DEFAULT_TOPLEVEL_RECOVERY);
            }
        }
    }

    private ClassDecl classDecl() {
        Token start = expect("class");
        Token nameTok = expect(IDENTIFIER);

        List<TypeParamDecl> typeParams = null;
        if (stream.matchSequence(COLON, COLON)) {
            expect(LANGLE);
            typeParams = stream.check(RANGLE) ? new ArrayList<>() : typeParameterList();
            expect(RANGLE);
        }

        expect(LPAREN);
        List<ParamDecl> parameters = stream.check(RPAREN) ? new ArrayList<>() : parameterList();
        expect(RPAREN);

        List<SuperDecl> supers = null;
        if (stream.match(COLON))
            supers = superList();

        List<MemberDecl> members = new ArrayList<>();
        Token end;

        if (stream.check(LBRACE)) {
            expect(LBRACE);
            members = memberList();
            expect(RBRACE);
            stream.match(SEMICOLON);
            end = stream.previous();
        } else if (stream.match(SEMICOLON)) {
            end = stream.previous();
        } else {
            throw error(EXPECTED_CLASS_BODY.format(), stream.peek());
        }

        return new ClassDecl(new Identifier(nameTok.lexeme, nameTok.position),
                parameters, supers, typeParams, members, between(start, end));
    }

    private List<MemberDecl> memberList() {
        List<MemberDecl> members = new ArrayList<>();
        while (!stream.isAtEnd() && !stream.check(RBRACE)) {
            try {
                MemberDecl member;
                if (stream.check("fun"))
                    member = funDecl();
                else if (stream.check("var"))
                    member = varDecl();
                else if (stream.check("operator"))
                    member = operatorDecl();
                else
                    throw error(EXPECTED_MEMBER.format(), stream.peek());
                members.add(member);
                semicolon();
            } catch (IllegalStateException e) {
                synchronizeAfter(DEFAULT_MEMBER_RECOVERY);
            }
        }
        return members;
    }

    private VarDecl varDecl() {
        Token start = expect("var");
        Token nameTok = expect(IDENTIFIER);
        Typed type = null;
        if (stream.match(COLON))
            type = type();
        Expr value = null;
        if (stream.matchSequence(MINUS, RANGLE))
            value = expr();
        return new VarDecl(new Identifier(nameTok.lexeme, nameTok.position),
                type, value, between(start, stream.previous()));
    }

    private FunDecl funDecl() {
        Token start = expect("fun");
        Token nameTok = expect(IDENTIFIER);

        List<TypeParamDecl> typeParams = null;
        if (stream.matchSequence(COLON, COLON)) {
            expect(LANGLE);
            typeParams = stream.check(RANGLE) ? new ArrayList<>() : typeParameterList();
            expect(RANGLE);
        }

        expect(LPAREN);
        List<ParamDecl> params = stream.check(RPAREN) ? new ArrayList<>() : parameterList();
        expect(RPAREN);

        Typed ret = null;
        if (stream.match(COLON))
            ret = type();

        BlockStmt body = null;
        if (stream.check(LBRACE))
            body = block();
        else if (!stream.check(SEMICOLON))
            throw error(EXPECTED_FUNCTION_BODY.format(), stream.peek());

        return new FunDecl(new Identifier(nameTok.lexeme, nameTok.position),
                ret, typeParams, params, body, span(start, body));
    }

    private OperatorDecl operatorDecl() {
        Token start = expect("operator");

        Token pFirst = stream.peek();
        Operator prefixOp = null;
        if (pFirst != null && pFirst.is(DEFAULT_OPERATOR_TYPE)) {
            StringBuilder sb = new StringBuilder();
            Token tk;
            while ((tk = stream.peek()) != null && tk.is(DEFAULT_OPERATOR_TYPE))
                sb.append(stream.advance().lexeme);
            prefixOp = new Operator(sb.toString(), pos(pFirst));
        }

        if (prefixOp != null) {
            ParamDecl operand = parameter();
            expect(COLON);
            Typed ret = type();
            BlockStmt body = null;
            if (stream.check(LBRACE))
                body = block();
            else if (!stream.check(SEMICOLON))
                throw error(EXPECTED_FUNCTION_BODY.format(), stream.peek());
            return new OperatorDecl(Operator.PREFIX, null, prefixOp, operand,
                    ret, body, null, span(start, body));
        }

        ParamDecl left = parameter();

        Token iFirst = stream.peek();
        Operator op = null;
        if (iFirst != null && iFirst.is(DEFAULT_OPERATOR_TYPE)) {
            StringBuilder sb = new StringBuilder();
            Token tk;
            while ((tk = stream.peek()) != null && tk.is(DEFAULT_OPERATOR_TYPE))
                sb.append(stream.advance().lexeme);
            op = new Operator(sb.toString(), pos(iFirst));
        }
        if (op == null)
            throw error("Expected operator symbol", stream.peek());

        if (stream.check(COLON)) {
            expect(COLON);
            Typed ret = type();
            BlockStmt body = null;
            if (stream.check(LBRACE))
                body = block();
            else if (!stream.check(SEMICOLON))
                throw error(EXPECTED_FUNCTION_BODY.format(), stream.peek());
            return new OperatorDecl(Operator.POSTFIX, left, op, null,
                    ret, body, null, span(start, body));
        }

        ParamDecl right = parameter();
        expect(COLON);
        Typed ret = type();
        BlockStmt body = null;
        if (stream.check(LBRACE))
            body = block();
        else if (!stream.check(SEMICOLON))
            throw error(EXPECTED_FUNCTION_BODY.format(), stream.peek());
        return new OperatorDecl(Operator.INFIX, left, op, right,
                ret, body, null, span(start, body));
    }

    // ==================== PARAMS ====================

    private ParamDecl parameter() {
        Token nameTok = expect(IDENTIFIER);
        Typed type = null;
        if (stream.match(COLON))
            type = type();
        Expr init = null;
        if (stream.matchSequence(MINUS, RANGLE))
            init = expr();
        Position end = init != null ? init.getPosition() : nameTok.position;
        return new ParamDecl(new Identifier(nameTok.lexeme, nameTok.position),
                type, init, end);
    }

    private List<ParamDecl> parameterList() {
        List<ParamDecl> params = new ArrayList<>();
        do {
            params.add(parameter());
        } while (stream.match(COMMA));
        return params;
    }

    private List<SuperDecl> superList() {
        List<SuperDecl> supers = new ArrayList<>();
        while (true) {
            Typed type = type();
            expect(LPAREN);
            List<Expr> args = stream.check(RPAREN) ? new ArrayList<>() : argumentList();
            expect(RPAREN);
            supers.add(new SuperDecl(type, args, pos(type)));
            if (stream.check(AMP)) {
                stream.advance();
                continue;
            }
            break;
        }
        return supers;
    }

    // ==================== TYPE PARAMS ====================

    private TypeParamDecl typeParameter() {
        Token nameTok = expect(IDENTIFIER);
        Identifier name = new Identifier(nameTok.lexeme, nameTok.position);
        List<BoundDecl> bounds = null;
        if (stream.match(LANGLE)) {
            bounds = stream.check(RANGLE) ? new ArrayList<>() : boundList();
            expect(RANGLE);
        }
        Positioned end = (bounds != null && !bounds.isEmpty())
                ? bounds.get(bounds.size() - 1)
                : nameTok;
        return new TypeParamDecl(name, bounds, between(nameTok, end));
    }

    private List<TypeParamDecl> typeParameterList() {
        List<TypeParamDecl> params = new ArrayList<>();
        do {
            params.add(typeParameter());
        } while (stream.match(COMMA));
        return params;
    }

    private BoundDecl bound() {
        Token nameTok = expect(IDENTIFIER);
        Identifier name = new Identifier(nameTok.lexeme, nameTok.position);
        List<TypeParamDecl> typeParams = null;
        if (stream.match(LANGLE)) {
            typeParams = stream.check(RANGLE) ? new ArrayList<>() : typeParameterList();
            expect(RANGLE);
        }
        Positioned end = typeParams != null ? stream.previous() : nameTok;
        return new BoundDecl(name, typeParams, between(nameTok, end));
    }

    private List<BoundDecl> boundList() {
        List<BoundDecl> bounds = new ArrayList<>();
        do {
            bounds.add(bound());
        } while (stream.match(COMMA));
        return bounds;
    }

    // ==================== TYPE ====================

    private Typed type() {
        Token nameTok = expect(IDENTIFIER);
        if (stream.matchSequence(COLON, COLON)) {
            expect(LANGLE);
            List<Typed> args = stream.check(RANGLE) ? new ArrayList<>() : typeArgumentList();
            expect(RANGLE);
            return new ParameterizedRefTyped(new Identifier(nameTok.lexeme, nameTok.position),
                    args, between(nameTok, stream.previous()));
        }
        return new RefTyped(new Identifier(nameTok.lexeme, nameTok.position), pos(nameTok));
    }

    private List<Typed> typeArgumentList() {
        List<Typed> args = new ArrayList<>();
        do {
            args.add(type());
        } while (stream.match(COMMA));
        return args;
    }

    // ==================== BLOCK / STMT ====================

    private BlockStmt block() {
        expect(LBRACE);
        Token start = stream.previous();
        List<Stmt> stmts = new ArrayList<>();
        while (!stream.isAtEnd() && !stream.check(RBRACE)) {
            try {
                Stmt s = stmt();
                if (s != null)
                    stmts.add(s);
            } catch (IllegalStateException e) {
                synchronizeAfter(DEFAULT_STMT_RECOVERY);
            }
        }
        Token end = expect(RBRACE);
        return new BlockStmt(stmts, between(start, end));
    }

    private Stmt stmt() {
        try {
            if (stream.check("let"))
                return letDecl();
            if (stream.check("if"))
                return ifStmt();
            if (stream.check("while"))
                return whileStmt();
            if (stream.match("break")) {
                Token t = stream.previous();
                semicolon();
                return new BreakStmt(pos(t));
            }
            if (stream.match("continue")) {
                Token t = stream.previous();
                semicolon();
                return new ContinueStmt(pos(t));
            }
            if (stream.match("return")) {
                Token t = stream.previous();
                if (stream.check(SEMICOLON)) {
                    semicolon();
                    return new ReturnStmt(null, pos(t));
                }
                Expr v = expr();
                semicolon();
                return new ReturnStmt(v, between(t, v));
            }
            return exprStmt();
        } catch (IllegalStateException e) {
            synchronizeAfter(DEFAULT_STMT_RECOVERY);
            return null;
        }
    }

    private LetStmt letDecl() {
        Token start = expect("let");
        Token nameTok = expect(IDENTIFIER);
        Typed type = null;
        if (stream.match(COLON))
            type = type();
        Expr value = null;
        if (stream.matchSequence(MINUS, RANGLE))
            value = expr();
        semicolon();
        return new LetStmt(new Identifier(nameTok.lexeme, nameTok.position),
                type, value, between(start, stream.previous()));
    }

    private IfStmt ifStmt() {
        Token start = expect("if");
        expect(LPAREN);
        Expr cond = expr();
        expect(RPAREN);

        BlockStmt thenB;
        if (stream.check(LBRACE)) {
            thenB = block();
        } else if (stream.matchSequence(MINUS, RANGLE)) {
            Expr e = expr();
            semicolon();
            List<Stmt> list = new ArrayList<>();
            list.add(new ExprStmt(e, pos(e)));
            thenB = new BlockStmt(list, between(start, e));
        } else
            throw error(EXPECTED_FUNCTION_BODY.format(), stream.peek());

        BlockStmt elseB = null;
        if (stream.match("else")) {
            if (stream.check(LBRACE)) {
                elseB = block();
            } else if (stream.matchSequence(MINUS, RANGLE)) {
                Expr e = expr();
                semicolon();
                List<Stmt> list = new ArrayList<>();
                list.add(new ExprStmt(e, pos(e)));
                elseB = new BlockStmt(list, between(start, e));
            } else
                throw error(EXPECTED_FUNCTION_BODY.format(), stream.peek());
        }

        return new IfStmt(cond, thenB, elseB,
                between(start, elseB != null ? elseB : thenB));
    }

    private WhileStmt whileStmt() {
        Token start = expect("while");
        expect(LPAREN);
        Expr cond = expr();
        expect(RPAREN);

        BlockStmt body;
        if (stream.check(LBRACE)) {
            body = block();
        } else if (stream.matchSequence(MINUS, RANGLE)) {
            Expr e = expr();
            semicolon();
            List<Stmt> list = new ArrayList<>();
            list.add(new ExprStmt(e, pos(e)));
            body = new BlockStmt(list, between(start, e));
        } else
            throw error(EXPECTED_FUNCTION_BODY.format(), stream.peek());

        return new WhileStmt(cond, body, between(start, body));
    }

    private ExprStmt exprStmt() {
        Expr e = expr();
        semicolon();
        return new ExprStmt(e, pos(e));
    }

    // ==================== EXPRESSIONS ====================

    private Expr expr() {
        return infix();
    }

    private Expr infix() {
        Expr left = prefix();
        while (true) {
            Token t = stream.peek();
            if (t == null || !t.is(DEFAULT_OPERATOR_TYPE))
                break;

            StringBuilder sb = new StringBuilder();
            Token tk;
            while ((tk = stream.peek()) != null && tk.is(DEFAULT_OPERATOR_TYPE))
                sb.append(stream.advance().lexeme);
            Operator op = new Operator(sb.toString(), pos(t));

            Token next = stream.peek();
            if (next != null && (next.is(DEFAULT_PRIMARY_TYPE) || next.is(DEFAULT_OPERATOR_TYPE))) {
                Expr right = prefix();
                left = new OperatorExpr(Operator.INFIX, left, op, right, null,
                        between(left, right));
            } else {
                left = new OperatorExpr(Operator.POSTFIX, left, op, null, null,
                        between(left, t));
            }
        }
        return left;
    }

    private Expr prefix() {
        Token first = stream.peek();
        if (first != null && first.is(DEFAULT_OPERATOR_TYPE)) {
            StringBuilder sb = new StringBuilder();
            Token tk;
            while ((tk = stream.peek()) != null && tk.is(DEFAULT_OPERATOR_TYPE))
                sb.append(stream.advance().lexeme);
            Operator op = new Operator(sb.toString(), pos(first));
            Expr right = prefix();
            return new OperatorExpr(Operator.PREFIX, null, op, right, null,
                    between(first, right));
        }
        return postfix();
    }

    private Expr postfix() {
        Expr expr = primary();
        while (true) {
            Token t = stream.peek();
            if (t == null)
                break;

            if (t.is(DOT)) {
                stream.advance();
                Token nameTok = expect(IDENTIFIER);
                Identifier id = new Identifier(nameTok.lexeme, nameTok.position);
                expr = new MemberExpr(expr, id, between(expr, nameTok));
                continue;
            }

            if (t.is(LPAREN)) {
                stream.advance();
                List<Expr> args = stream.check(RPAREN) ? new ArrayList<>() : argumentList();
                expect(RPAREN);
                expr = new CallExpr(expr, null, args, between(expr, stream.previous()));
                continue;
            }

            if (stream.matchSequence(COLON, COLON)) {
                List<Typed> typeArgs = null;
                if (stream.match(LANGLE)) {
                    typeArgs = stream.check(RANGLE) ? new ArrayList<>() : typeArgumentList();
                    expect(RANGLE);
                }
                if (stream.check(LPAREN)) {
                    stream.advance();
                    List<Expr> args = stream.check(RPAREN) ? new ArrayList<>() : argumentList();
                    expect(RPAREN);
                    expr = new CallExpr(expr, typeArgs, args, between(expr, stream.previous()));
                    continue;
                }
                throw error("Expected '(' after '::'", stream.peek());
            }

            break;
        }
        return expr;
    }

    private Expr primary() {
        Token t = stream.peek();
        if (t == null || !t.is(DEFAULT_PRIMARY_TYPE))
            throw error(UNEXPECTED_TOKEN.format(t != null ? t.lexeme : "EOF"), t);

        stream.advance();

        if (t.is(NULL, BOOL, INT, FLOAT, STRING)) {
            int kind;
            switch (t.type) {
                case NULL:
                    kind = LiteralExpr.NULL;
                    break;
                case BOOL:
                    kind = LiteralExpr.BOOL;
                    break;
                case INT:
                    kind = LiteralExpr.INT;
                    break;
                case FLOAT:
                    kind = LiteralExpr.FLOAT;
                    break;
                case STRING:
                    kind = LiteralExpr.STRING;
                    break;
                default:
                    throw new IllegalStateException("Unexpected literal: " + t.type);
            }
            return new LiteralExpr(kind, t.lexeme, pos(t));
        }
        if (t.is(IDENTIFIER))
            return new RefExpr(new Identifier(t.lexeme, pos(t)), pos(t));
        if (t.is(LPAREN)) {
            Expr inner = expr();
            expect(RPAREN);
            return new ParenthesisExpr(inner, pos(inner));
        }
        throw error(UNEXPECTED_TOKEN.format(t.lexeme), t);
    }

    private List<Expr> argumentList() {
        List<Expr> args = new ArrayList<>();
        do {
            args.add(expr());
        } while (stream.match(COMMA));
        return args;
    }

    public TokenStream getStream() {
        return stream;
    }

    public CompilationUnit getCompilationUnit() {
        return unit;
    }
}