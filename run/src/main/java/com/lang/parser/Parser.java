package com.lang.parser;

import static com.lang.parser.ParsingErrorCode.*;
import static com.lang.token.Type.*;

import com.lang.ast.*;
import com.lang.token.*;
import com.lang.unit.*;
import com.lang.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Parser {
	private static final Set<String> DEFAULT_STMT_RECOVERY = Set.of(
			"let", "if", "while", "break", "continue", "return");

	private static final Set<String> DEFAULT_MEMBER_RECOVERY = Set.of(
			"fun", "var", "}");

	private final TokenStream stream;
	private final CompilationUnit unit;

	public Parser(TokenStream stream, CompilationUnit unit) {
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

	public CompilationUnit parse() {
		program();
		return unit;
	}

	private void program() {
		while (!stream.isAtEnd() && !stream.check(EOF)) {
			if (stream.check("class")) {
				unit.addClass(classDecl());
			} else {
				throw unit.error(
						TAG,
						EXPECTED_TOKEN.format("class", stream.peek().type),
						stream.peek());
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void synchronizeAfter(Set recoveryPoints) {
		while (!stream.isAtEnd() && !stream.check(EOF)) {
			if (stream.match(SEMICOLON))
				return;
			for (Object type : recoveryPoints) {
				if (type instanceof Type) {
					if (stream.check((Type) type))
						return;
				}
				if (type instanceof String) {
					if (stream.check((String) type))
						return;
				}
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
			unit.addError(
					unit.error(
							TAG,
							EXPECTED_TOKEN.format(";", stream.peek().type),
							stream.peek()));
		}
	}

	private BlockStmt block() {
		stream.expect(LBRACE);

		Token start = stream.previous();
		List<Stmt> statements = new ArrayList<>();

		while (!stream.isAtEnd() && !stream.check(RBRACE)) {
			try {
				Stmt s = stmt();
				if (s != null)
					statements.add(s);
			} catch (CompilationException e) {
				unit.addError(e);
				synchronizeAfter(DEFAULT_STMT_RECOVERY);
			} catch (IllegalStateException e) {
				unit.addError(
						unit.error(
								TAG,
								e.getMessage(),
								stream.peek()));
				synchronizeAfter(DEFAULT_STMT_RECOVERY);
			}
		}
		Token end = stream.expect(RBRACE);

		return new BlockStmt(statements, between(start, end));

	}

	private VarDeclStmt varDecl() {
		Token start = stream.expect("var");

		Token nameToken = stream.expect(IDENTIFIER);

		Typed type = null;
		if (stream.match(COLON))
			type = type();

		Expr value = null;
		if (stream.match(EQUALS))
			value = expr();

		semicolon();

		VarDeclStmt var = new VarDeclStmt();
		var.name = new Identifier(nameToken.lexeme, nameToken.position);
		var.type = type;
		var.value = value;
		var.position = between(start, stream.previous());
		return var;
	}

	private FunDeclStmt funDecl() {
		Token start = stream.expect("fun");

		Token nameToken = stream.expect(IDENTIFIER);

		List<TypeParamDeclStmt> typeParameters = null;
		if (stream.check(LANGLE))
			typeParameters = typeParameters();

		List<ParamDeclStmt> params = parameters();

		Typed returnType = null;
		if (stream.match(COLON))
			returnType = type();

		BlockStmt body = null;

		if (stream.match(SEMICOLON)) {
			body = null;
		} else if (stream.match(EQUALS)) {
			if (stream.match(QUESTION)) {
				semicolon();
				body = new BlockStmt(List.of(), pos(stream.previous()));
			} else {
				Expr value = expr();
				semicolon();
				ReturnStmt ret = new ReturnStmt(value, pos(value));
				body = new BlockStmt(List.of(ret), pos(ret));
			}
		} else if (stream.check(LBRACE)) {
			body = block();
		} else {
			throw unit.error(
					TAG,
					EXPECTED_FUNCTION_BODY.format(),
					stream.peek());
		}

		return new FunDeclStmt(new Identifier(nameToken.lexeme, nameToken.position), typeParameters, params, returnType,
				body,
				between(start, body != null ? body : stream.previous()));
	}

	private List<ParamDeclStmt> parameters() {
		stream.expect(LPAREN);

		List<ParamDeclStmt> params = new ArrayList<>();

		if (!stream.check(RPAREN)) {
			do {
				Token nameToken = stream.expect(IDENTIFIER);
				Typed type = null;
				if (stream.match(COLON))
					type = type();

				params.add(
						new ParamDeclStmt(new Identifier(nameToken.lexeme, nameToken.position), type, pos(nameToken)));
			} while (stream.match(COMMA));
		}

		stream.expect(RPAREN);
		return params;
	}

	private List<Typed> superclasses() {
		List<Typed> superclasses = new ArrayList<>();

		if (stream.match(COLON)) {
			do {
				superclasses.add(type());
			} while (stream.match(AMP));
		}

		return superclasses;
	}

	private List<TypeParamDeclStmt> typeParameters() {
		stream.expect(LANGLE);

		List<TypeParamDeclStmt> params = new ArrayList<>();

		do {
			Token nameToken = stream.expect(IDENTIFIER);

			List<Typed> superclasses = superclasses();

			Positioned end = !superclasses.isEmpty()
					? superclasses.get(superclasses.size() - 1)
					: nameToken;

			params.add(
					new TypeParamDeclStmt(
							new Identifier(nameToken.lexeme, nameToken.position),
							superclasses,
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
					new Identifier(nameToken.lexeme, nameToken.position),
					types,
					between(nameToken, stream.previous()));
		}

		return new RefTyped(new Identifier(nameToken.lexeme, nameToken.position), pos(nameToken));
	}

	private Stmt stmt() {
		Set<String> stmtRecovery = DEFAULT_STMT_RECOVERY;

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

		} catch (CompilationException e) {
			unit.addError(e);
			synchronizeAfter(stmtRecovery);
			return null;
		} catch (IllegalStateException e) {
			unit.addError(
					unit.error(
							TAG,
							e.getMessage(),
							stream.peek()));
			synchronizeAfter(stmtRecovery);
			return null;
		}
	}

	private ClassDeclStmt classDecl() {
		Token start = stream.expect("class");

		Token nameToken = stream.expect(IDENTIFIER);

		List<TypeParamDeclStmt> typeParameters = null;
		if (stream.check(LANGLE))
			typeParameters = typeParameters();

		List<Typed> superclasses = superclasses();

		List<FunDeclStmt> funs = new ArrayList<>();
		List<VarDeclStmt> vars = new ArrayList<>();
		Positioned end = null;

		if (stream.match(SEMICOLON)) {
			end = stream.previous();
		} else {
			stream.expect(LBRACE);

			while (!stream.isAtEnd() && !stream.check(RBRACE)) {
				try {
					if (stream.check("fun")) {
						FunDeclStmt fun = funDecl();
						funs.add(fun);
					} else if (stream.check("var")) {
						VarDeclStmt var = varDecl();
						vars.add(var);

					} else {
						throw unit.error(
								TAG,
								EXPECTED_MEMBER.format(),
								stream.peek());
					}
				} catch (CompilationException e) {
					unit.addError(e);
					synchronizeAfter(DEFAULT_MEMBER_RECOVERY);
				} catch (IllegalStateException e) {
					unit.addError(
							unit.error(
									TAG,
									e.getMessage(),
									stream.peek()));
					synchronizeAfter(DEFAULT_MEMBER_RECOVERY);
				}
			}

			stream.expect(RBRACE);
			end = stream.previous();
		}

		return new ClassDeclStmt(new Identifier(nameToken.lexeme, nameToken.position), superclasses,
				typeParameters, funs, vars, between(start, end));
	}

	private LetDeclStmt letDecl() {
		Token start = stream.expect("let");

		Token nameToken = stream.expect(IDENTIFIER);

		Typed type = null;
		if (stream.match(COLON))
			type = type();

		Expr value = null;
		if (stream.match(EQUALS))
			value = expr();

		semicolon();

		return new LetDeclStmt(new Identifier(nameToken.lexeme, nameToken.position), type, value,
				between(start, stream.previous()));
	}

	private IfStmt ifStmt() {
		Token start = stream.expect("if");

		stream.expect(LPAREN);
		Expr condition = expr();
		stream.expect(RPAREN);

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
		Token start = stream.expect("while");

		stream.expect(LPAREN);
		Expr condition = expr();
		stream.expect(RPAREN);
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
		Expr expr = ternary();
		boolean strict = stream.match(DOLLAR);

		if (stream.match(EQUALS)) {
			Expr value = assignment();
			return new AssignExpr(expr, new Operator(strict, "=", pos(stream.previous())), value, between(expr, value));
		}

		if (strict) {
			throw unit.error(
					TAG,
					EXPECTED_TOKEN.format("= after $", stream.peek().type),
					stream.peek());
		}

		return expr;
	}

	private Expr ternary() {
		Expr expr = logicalOr();
		if (stream.match(QUESTION)) {
			Expr thenExpr = expr();
			stream.expect(COLON);
			Expr elseExpr = ternary();
			return new TernaryExpr(expr, thenExpr, elseExpr, between(expr, elseExpr));
		}
		return expr;
	}

	private Expr logicalOr() {
		Expr expr = logicalAnd();
		while (stream.matchSequence(BAR, BAR)) {
			Expr right = logicalAnd();
			expr = new BinaryExpr(expr, new Operator(false, "||", pos(stream.previous())), right, between(expr, right));
		}
		return expr;
	}

	private Expr logicalAnd() {
		Expr expr = equality();
		while (stream.matchSequence(AMP, AMP)) {
			Expr right = equality();
			expr = new BinaryExpr(expr, new Operator(false, "&&", pos(stream.previous())), right, between(expr, right));
		}
		return expr;
	}

	private Expr equality() {
		Expr expr = comparison();
		while (true) {
			boolean strict = stream.match(DOLLAR);

			if (stream.matchSequence(EQUALS, EQUALS)) {
				Expr right = comparison();
				expr = new BinaryExpr(expr, new Operator(strict, "==", pos(stream.previous())), right,
						between(expr, right));
			} else if (stream.matchSequence(BANG, EQUALS)) {
				Expr right = comparison();
				expr = new BinaryExpr(expr, new Operator(strict, "!=", pos(stream.previous())), right,
						between(expr, right));
			} else if (strict) {
				throw unit.error(
						TAG,
						EXPECTED_TOKEN.format("== or != after $", stream.peek().type),
						stream.peek());
			} else {
				break;
			}
		}
		return expr;
	}

	private Expr comparison() {
		Expr expr = term();
		while (true) {
			boolean strict = stream.match(DOLLAR);

			if (stream.matchSequence(LANGLE, EQUALS)) {
				Expr right = term();
				expr = new BinaryExpr(expr, new Operator(strict, "<=", pos(stream.previous())), right,
						between(expr, right));
			} else if (stream.matchSequence(RANGLE, EQUALS)) {
				Expr right = term();
				expr = new BinaryExpr(expr, new Operator(strict, ">=", pos(stream.previous())), right,
						between(expr, right));
			} else if (stream.match(LANGLE)) {
				Expr right = term();
				expr = new BinaryExpr(expr, new Operator(strict, "<", pos(stream.previous())), right,
						between(expr, right));
			} else if (stream.match(RANGLE)) {
				Expr right = term();
				expr = new BinaryExpr(expr, new Operator(strict, ">", pos(stream.previous())), right,
						between(expr, right));
			} else if (strict) {
				throw unit.error(
						TAG,
						EXPECTED_TOKEN.format("comparison operator after $", stream.peek().type),
						stream.peek());
			} else {
				break;
			}
		}
		return expr;
	}

	private Expr term() {
		Expr expr = factor();
		while (stream.checkAny(PLUS, MINUS, DOLLAR)) {
			boolean strict = stream.match(DOLLAR);

			if (stream.checkAny(PLUS, MINUS)) {
				Token op = stream.advance();
				Expr right = factor();
				expr = new BinaryExpr(expr, new Operator(strict, op.lexeme, pos(op)), right, between(expr, right));
			} else if (strict) {
				throw unit.error(
						TAG,
						EXPECTED_TOKEN.format("+ or - after $", stream.peek().type),
						stream.peek());
			}
		}
		return expr;
	}

	private Expr factor() {
		Expr expr = unary();
		while (stream.checkAny(STAR, SLASH, PERCENT, DOLLAR)) {
			boolean strict = stream.match(DOLLAR);

			if (stream.checkAny(STAR, SLASH, PERCENT)) {
				Token op = stream.advance();
				Expr right = unary();
				expr = new BinaryExpr(expr, new Operator(strict, op.lexeme, pos(op)), right, between(expr, right));
			} else if (strict) {
				throw unit.error(
						TAG,
						EXPECTED_TOKEN.format("*, / or % after $", stream.peek().type),
						stream.peek());
			}
		}
		return expr;
	}

	private Expr unary() {
		boolean strict = stream.match(DOLLAR);

		if (stream.match(BANG)) {
			return new UnaryExpr(new Operator(strict, "!", pos(stream.previous())), unary(),
					between(stream.previous(), stream.peek()));
		}
		if (stream.match(MINUS)) {
			return new UnaryExpr(new Operator(strict, "-", pos(stream.previous())), unary(),
					between(stream.previous(), stream.peek()));
		}
		if (stream.match(PLUS)) {
			return new UnaryExpr(new Operator(strict, "+", pos(stream.previous())), unary(),
					between(stream.previous(), stream.peek()));
		}

		if (strict) {
			throw unit.error(
					TAG,
					EXPECTED_TOKEN.format("!, - or + after $", stream.peek().type),
					stream.peek());
		}

		return access();
	}

	private Expr access() {
		Expr expr = primary();

		while (true) {
			if (stream.match(LBRACKET)) {
				Expr index = expr();
				stream.expect(RBRACKET);
				expr = new IndexAccessExpr(expr, index, between(expr, stream.previous()));
				continue;
			}

			List<Typed> typeArguments = null;
			if (stream.check(LANGLE)) {
				if (stream.checkNext(RANGLE) ||
						(stream.checkNext(IDENTIFIER) && stream.checkNextNextAny(LANGLE, RANGLE, COMMA))) {
					try {
						typeArguments = typeArguments();
						if (!stream.check(LPAREN)) {
							typeArguments = null;
						}
					} catch (CompilationException e) {
						typeArguments = null;
					} catch (IllegalStateException e) {
						typeArguments = null;
					}
				}
			}

			if (stream.check(LPAREN)) {
				List<Expr> args = arguments();
				Positioned endPos = !args.isEmpty() ? args.get(args.size() - 1) : expr;
				expr = new CallExpr(expr, typeArguments, args, between(expr, endPos));
				continue;
			}

			if (stream.match(DOT)) {
				Token nameToken = stream.expect(IDENTIFIER);
				expr = new MemberAccessExpr(expr, new Identifier(nameToken.lexeme, nameToken.position),
						between(expr, nameToken));
				continue;
			}

			break;
		}

		return expr;
	}

	private List<Expr> arguments() {
		stream.expect(LPAREN);

		List<Expr> args = new ArrayList<>();
		if (!stream.check(RPAREN)) {
			do {
				args.add(expr());
			} while (stream.match(COMMA));
		}

		stream.expect(RPAREN);
		return args;
	}

	private Expr primary() {
		Token t = stream.advance();

		if (t.isLiteral()) {
			int literalType;
			String value = t.lexeme;

			switch (t.type) {
				case NULL:
					literalType = LiteralExpr.NULL;
					break;
				case BOOL:
					literalType = LiteralExpr.BOOL;
					break;
				case CHAR:
					literalType = LiteralExpr.CHAR;
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
			stream.expect(RPAREN);
			return new ParenthesisExpr(inner, pos(inner));
		}

		throw unit.error(
				TAG,
				UNEXPECTED_TOKEN.format(t.lexeme),
				t);
	}

	public TokenStream getStream() {
		return stream;
	}

	public CompilationUnit getCompilationUnit() {
		return unit;
	}
}