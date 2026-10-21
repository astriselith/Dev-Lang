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
			"fun", "var", "}");

	private static final Set<String> DEFAULT_CLASS_RECOVERY = Set.of("class");

	private final TokenStream stream;
	private final CompilationUnit unit;

	public Parser(TokenStream stream, CompilationUnit unit) {
		this.stream = stream;
		this.unit = unit;

		if (this.stream == null) {
			throw new IllegalArgumentException("TokenStream cannot be null");
		}

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
		Token token = stream.peek();
		if (token == null || !token.typeEquals(type)) {
			throw error(
					EXPECTED_TOKEN.format(
							Token.typeName(type),
							Token.typeName(token != null ? token.type : Token.UNDEFINED)),
					token);
		}
		return stream.advance();
	}

	private Token expect(String lexeme) {
		Token token = stream.peek();
		if (token == null || !token.lexemeEquals(lexeme)) {
			throw error(
					EXPECTED_TOKEN.format(
							"\"" + lexeme + "\"",
							Token.typeName(token != null ? token.type : Token.UNDEFINED)),
					token);
		}
		return stream.advance();
	}

	private void synchronizeAfter(Set<String> recoveryPoints) {
		while (!stream.isAtEnd() && !stream.check(EOF)) {
			if (stream.match(SEMICOLON))
				return;
			for (String lexeme : recoveryPoints) {
				if (stream.check(lexeme))
					return;
			}
			stream.advance();
		}
	}

	private Position pos(Positioned p) {
		return p.getPosition();
	}

	private Position between(Positioned start, Positioned end) {
		return Position.between(start.getPosition(), end.getPosition());
	}

	private void semicolon() {
		if (!stream.match(SEMICOLON)) {
			Token token = stream.peek();
			unit.addDiagnostic(TAG, Diagnostic.error(token.position,
					EXPECTED_TOKEN.format(";", Token.typeName(token.type))));
		}
	}

	private void program() {
		while (!stream.isAtEnd() && !stream.check(EOF)) {
			try {
				if (stream.check("class")) {
					unit.addClass(classDecl());
				} else {
					throw error(
							EXPECTED_TOKEN.format("class", Token.typeName(stream.peek().type)),
							stream.peek());
				}
			} catch (IllegalStateException e) {
				synchronizeAfter(DEFAULT_CLASS_RECOVERY);
			}
		}
	}

	private ClassDecl classDecl() {
		Token start = expect("class");

		Token nameToken = expect(IDENTIFIER);

		List<TypeParamDecl> typeParameters = null;
		if (stream.match(LANGLE)) {
			typeParameters = stream.check(RANGLE)
					? new ArrayList<>()
					: typeParameterList();
			expect(RANGLE);
		}

		expect(LPAREN);
		List<ParamDecl> parameters = stream.check(RPAREN)
				? new ArrayList<>()
				: parameterList();
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
			semicolon();
			end = stream.previous();
		} else if (stream.match(SEMICOLON)) {
			end = stream.previous();
		} else {
			throw error(EXPECTED_CLASS_BODY.format(), stream.peek());
		}

		return new ClassDecl(new Identifier(nameToken.lexeme, nameToken.position),
				parameters, supers, typeParameters, members, between(start, end));
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
		Token nameToken = expect(IDENTIFIER);

		Typed type = null;
		if (stream.match(COLON))
			type = type();

		Expr value = null;
		if (stream.match(EQUALS))
			value = expr();

		return new VarDecl(new Identifier(nameToken.lexeme, nameToken.position),
				type, value,
				between(start, stream.previous()));
	}

	private FunDecl funDecl() {
		Token start = expect("fun");

		Token nameToken = expect(IDENTIFIER);

		List<TypeParamDecl> typeParameters = null;
		if (stream.match(LANGLE)) {
			typeParameters = stream.check(RANGLE)
					? new ArrayList<>()
					: typeParameterList();
			expect(RANGLE);
		}

		expect(LPAREN);
		List<ParamDecl> params = stream.check(RPAREN)
				? new ArrayList<>()
				: parameterList();
		expect(RPAREN);

		Typed type = null;
		if (stream.match(COLON))
			type = type();

		BlockStmt body = null;
		if (stream.check(LBRACE)) {
			body = block();
		} else if (!stream.check(SEMICOLON)) {
			throw error(EXPECTED_FUNCTION_BODY.format(), stream.peek());
		}

		return new FunDecl(new Identifier(nameToken.lexeme, nameToken.position), type, typeParameters, params,
				body,
				between(start, body != null ? body : stream.previous()));
	}

	private List<ParamDecl> parameterList() {
		List<ParamDecl> params = new ArrayList<>();

		do {
			Token nameToken = expect(IDENTIFIER);
			Typed type = null;
			if (stream.match(COLON))
				type = type();

			Expr init = null;
			if (stream.match(EQUALS)) {
				init = expr();
			}

			Position end = init != null
					? init.getPosition()
					: nameToken.position;

			params.add(
					new ParamDecl(new Identifier(nameToken.lexeme, nameToken.position), type, init,
							end));
		} while (stream.match(COMMA));

		return params;
	}

	private List<SuperDecl> superList() {
		List<SuperDecl> supers = new ArrayList<>();

		do {
			Typed type = type();

			expect(LPAREN);
			List<Expr> arguments = stream.check(RPAREN)
					? new ArrayList<>()
					: argumentList();
			expect(RPAREN);

			supers.add(new SuperDecl(type, arguments, pos(type)));
		} while (stream.match(AMP));

		return supers;
	}

private List<BoundDecl> boundList() {
    List<BoundDecl> bounds = new ArrayList<>();

    do {
        Typed type = type();
        bounds.add(new BoundDecl(type, pos(type)));
    } while (stream.match(AMP));

    return bounds;
}

	private List<TypeParamDecl> typeParameterList() {
		List<TypeParamDecl> params = new ArrayList<>();

		do {
			Token nameToken = expect(IDENTIFIER);

			List<BoundDecl> bounds = null;
			if (stream.match(COLON))
				bounds = boundList();

			Positioned end = bounds != null
					? bounds.get(bounds.size() - 1)
					: nameToken;

			params.add(
					new TypeParamDecl(
							new Identifier(nameToken.lexeme, nameToken.position),
							bounds,
							between(nameToken, end)));
		} while (stream.match(COMMA));

		return params;
	}

	private List<Typed> typeArgumentList() {
		List<Typed> args = new ArrayList<>();

		do {
			args.add(type());
		} while (stream.match(COMMA));

		return args;
	}

	private Typed type() {
		Token nameToken = expect(IDENTIFIER);

		if (stream.match(LANGLE)) {
			List<Typed> types = stream.check(RANGLE)
					? new ArrayList<>()
					: typeArgumentList();
			expect(RANGLE);
			return new ParameterizedRefTyped(
					new Identifier(nameToken.lexeme, nameToken.position),
					types,
					between(nameToken, stream.previous()));
		}

		return new RefTyped(new Identifier(nameToken.lexeme, nameToken.position), pos(nameToken));
	}

	private BlockStmt block() {
		expect(LBRACE);

		Token start = stream.previous();
		List<Stmt> statements = new ArrayList<>();

		while (!stream.isAtEnd() && !stream.check(RBRACE)) {
			try {
				Stmt s = stmt();
				if (s != null)
					statements.add(s);
			} catch (IllegalStateException e) {
				synchronizeAfter(DEFAULT_STMT_RECOVERY);
			}
		}
		Token end = expect(RBRACE);

		return new BlockStmt(statements, between(start, end));
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

				Expr value = expr();
				semicolon();

				return new ReturnStmt(
						value,
						between(t, value));
			}

			return exprStmt();

		} catch (IllegalStateException e) {
			synchronizeAfter(DEFAULT_STMT_RECOVERY);
			return null;
		}
	}

	private LetStmt letDecl() {
		Token start = expect("let");

		Token nameToken = expect(IDENTIFIER);

		Typed type = null;
		if (stream.match(COLON))
			type = type();

		Expr value = null;
		if (stream.match(EQUALS))
			value = expr();

		semicolon();

		return new LetStmt(new Identifier(nameToken.lexeme, nameToken.position), type, value,
				between(start, stream.previous()));
	}

	private IfStmt ifStmt() {
		Token start = expect("if");

		expect(LPAREN);
		Expr condition = expr();
		expect(RPAREN);

		BlockStmt thenBranch;
		if (stream.check(LBRACE)) {
			thenBranch = block();
		} else {
			List<Stmt> thenList = new ArrayList<>();
			thenList.add(stmt());
			thenBranch = new BlockStmt(
					thenList,
					between(start, thenList.get(thenList.size() - 1)));
		}

		BlockStmt elseBranch = null;

		if (stream.match("else")) {
			if (stream.check(LBRACE)) {
				elseBranch = block();
			} else {
				List<Stmt> elseList = new ArrayList<>();
				elseList.add(stmt());
				elseBranch = new BlockStmt(
						elseList,
						between(start, elseList.get(elseList.size() - 1)));
			}
		}

		Positioned endPos = elseBranch != null ? elseBranch : thenBranch;

		return new IfStmt(
				condition,
				thenBranch,
				elseBranch,
				between(start, endPos));
	}

	private WhileStmt whileStmt() {
		Token start = expect("while");

		expect(LPAREN);
		Expr condition = expr();
		expect(RPAREN);
		BlockStmt body;

		if (stream.check(LBRACE)) {
			body = block();
		} else {
			List<Stmt> bodyList = new ArrayList<>();
			bodyList.add(stmt());
			body = new BlockStmt(
					bodyList,
					between(start, bodyList.get(bodyList.size() - 1)));
		}

		return new WhileStmt(
				condition,
				body,
				between(start, body));
	}

	private ExprStmt exprStmt() {
		Expr e = expr();
		semicolon();
		return new ExprStmt(e, pos(e));
	}

	private Expr expr() {
		return assignment();
	}

	private Expr assignment() {
		Expr expr = logicalOr();

		if (stream.match(EQUALS)) {
			Expr value = assignment();
			return new AssignExpr(expr, new Operator("=", pos(stream.previous())), value, between(expr, value));
		}

		return expr;
	}

	private Expr logicalOr() {
		Expr expr = logicalAnd();
		while (stream.matchSequence(BAR, BAR)) {
			Expr right = logicalAnd();
			expr = new BinaryExpr(expr, new Operator("||", pos(stream.previous())), right, between(expr, right));
		}
		return expr;
	}

	private Expr logicalAnd() {
		Expr expr = equality();
		while (stream.matchSequence(AMP, AMP)) {
			Expr right = equality();
			expr = new BinaryExpr(expr, new Operator("&&", pos(stream.previous())), right, between(expr, right));
		}
		return expr;
	}

	private Expr equality() {
		Expr expr = comparison();
		while (true) {
			if (stream.matchSequence(EQUALS, EQUALS)) {
				Expr right = comparison();
				expr = new BinaryExpr(expr, new Operator("==", pos(stream.previous())), right,
						between(expr, right));
			} else if (stream.matchSequence(BANG, EQUALS)) {
				Expr right = comparison();
				expr = new BinaryExpr(expr, new Operator("!=", pos(stream.previous())), right,
						between(expr, right));
			} else {
				break;
			}
		}
		return expr;
	}

	private Expr comparison() {
		Expr expr = term();

		if (stream.matchSequence(LANGLE, EQUALS)) {
			Expr right = term();
			return new BinaryExpr(expr, new Operator("<=", pos(stream.previous())), right,
					between(expr, right));
		} else if (stream.matchSequence(RANGLE, EQUALS)) {
			Expr right = term();
			return new BinaryExpr(expr, new Operator(">=", pos(stream.previous())), right,
					between(expr, right));
		}

		return expr;
	}

	private Expr term() {
		Expr expr = factor();
		while (stream.checkAny(PLUS, MINUS)) {
			Token op = stream.advance();
			Expr right = factor();
			expr = new BinaryExpr(expr, new Operator(op.lexeme, pos(op)), right, between(expr, right));
		}
		return expr;
	}

	private Expr factor() {
		Expr expr = unary();
		while (stream.checkAny(STAR, SLASH, PERCENT)) {
			Token op = stream.advance();
			Expr right = unary();
			expr = new BinaryExpr(expr, new Operator(op.lexeme, pos(op)), right, between(expr, right));
		}
		return expr;
	}

	private Expr unary() {
		if (stream.match(BANG)) {
			return new UnaryExpr(new Operator("!", pos(stream.previous())), unary(),
					between(stream.previous(), stream.peek()));
		}
		if (stream.match(MINUS)) {
			return new UnaryExpr(new Operator("-", pos(stream.previous())), unary(),
					between(stream.previous(), stream.peek()));
		}
		if (stream.match(PLUS)) {
			return new UnaryExpr(new Operator("+", pos(stream.previous())), unary(),
					between(stream.previous(), stream.peek()));
		}

		return call();
	}

	private Expr call() {
		Expr expr = primary();

		while (true) {
			if (stream.match(DOT)) {
				Token nameToken = expect(IDENTIFIER);
				expr = new MemberAccessExpr(
						expr,
						new Identifier(nameToken.lexeme, nameToken.position),
						between(expr, nameToken));
				continue;
			}

			if (stream.check(LANGLE) || stream.check(LPAREN)) {
				List<Typed> typeArguments = null;

				if (stream.check(LANGLE)
						&& (stream.checkIndex(1, RANGLE)
								|| (stream.checkIndex(1, IDENTIFIER)
										&& stream.checkIndexAny(2, RANGLE, COMMA, LANGLE)))) {
					expect(LANGLE);
					typeArguments = stream.check(RANGLE)
							? new ArrayList<>()
							: typeArgumentList();
					expect(RANGLE);

					if (!stream.check(LPAREN)) {
						throw error("Expected '(' after type arguments", stream.peek());
					}
				} else if (stream.check(LANGLE)) {
					break;
				}

				expect(LPAREN);
				List<Expr> args = stream.check(RPAREN)
						? new ArrayList<>()
						: argumentList();
				expect(RPAREN);

				Positioned endPos = !args.isEmpty()
						? args.get(args.size() - 1)
						: expr;

				expr = new CallExpr(expr, typeArguments, args, between(expr, endPos));
				continue;
			}

			break;
		}

		return expr;
	}

	private List<Expr> argumentList() {
		List<Expr> args = new ArrayList<>();

		do {
			args.add(expr());
		} while (stream.match(COMMA));

		return args;
	}

	private Expr primary() {
		Token t = stream.advance();

		if (t.is(NULL, BOOL, INT, FLOAT, STRING)) {
			int literalType;
			String value = t.lexeme;

			switch (t.type) {
				case NULL:
					literalType = LiteralExpr.NULL;
					break;
				case BOOL:
					literalType = LiteralExpr.BOOL;
					break;
				case INT:
					literalType = LiteralExpr.INT;
					break;
				case FLOAT:
					literalType = LiteralExpr.FLOAT;
					break;
				case STRING:
					literalType = LiteralExpr.STRING;
					break;
				default:
					throw new IllegalStateException("Unexpected literal type: " + t.type);
			}

			return new LiteralExpr(literalType, value, pos(t));
		}

		if (t.typeEquals(IDENTIFIER)) {
			return new RefExpr(new Identifier(t.lexeme, pos(t)), pos(t));
		}

		if (t.typeEquals(LPAREN)) {
			Expr inner = expr();
			expect(RPAREN);
			return new ParenthesisExpr(inner, pos(inner));
		}

		throw error(UNEXPECTED_TOKEN.format(t.lexeme), t);
	}

	public TokenStream getStream() {
		return stream;
	}

	public CompilationUnit getCompilationUnit() {
		return unit;
	}
}