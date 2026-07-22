package com.lang.token;

import com.lang.buffer.ObjectBuffer;
import com.lang.buffer.ObjectHandler;

public abstract class TokenStream extends ObjectBuffer<Token> {

	@Override
	public ObjectHandler<Token> getHandler() {
		return handler;
	}

	@Override
	public void setHandler(ObjectHandler<Token> handler) {
		this.handler = handler;
	}

	protected TokenStream() {
		this(10, 10, 4);
	}

	protected TokenStream(int forwardWindow, int backwardWindow, int capacityMultiplier) {
		super(forwardWindow, backwardWindow, capacityMultiplier);
	}

	@Override
	protected abstract Token fetchNext();

	@Override
	protected boolean isEOF(Token token) {
		return token == null || token.type == Type.EOF;
	}

	// Métodos de acesso básicos

	public Token peek() {
		return offset(0);
	}

	public Token peekNext() {
		return offset(1);
	}

	public Token peekNextNext() {
		return offset(2);
	}

	public Token peekBack() {
		return offset(-1);
	}

	public Token peekBackBack() {
		return offset(-2);
	}

	public Token advance() {
		return next();
	}

	public Token previous() {
		return offset(-1);
	}

	// Verificações por tipo

	public boolean check(Type type) {
		Token t = offset(0);
		return t != null && t.type == type;
	}

	public boolean check(int index, Type type) {
		Token t = offset(index);
		return t != null && t.type == type;
	}

	public boolean checkNext(Type type) {
		return check(1, type);
	}

	public boolean checkNextNext(Type type) {
		return check(2, type);
	}

	public boolean checkBack(Type type) {
		return check(-1, type);
	}

	public boolean checkBackBack(Type type) {
		return check(-2, type);
	}

	public boolean checkAny(Type... types) {
		if (types == null || types.length == 0)
			return false;
		Token t = offset(0);
		if (t == null)
			return false;
		for (Type type : types) {
			if (t.type == type)
				return true;
		}
		return false;
	}

	public boolean checkAny(int index, Type... types) {
		if (types == null || types.length == 0)
			return false;
		Token t = offset(index);
		if (t == null)
			return false;
		for (Type type : types) {
			if (t.type == type)
				return true;
		}
		return false;
	}

	public boolean checkNextAny(Type... types) {
		return checkAny(1, types);
	}

	public boolean checkNextNextAny(Type... types) {
		return checkAny(2, types);
	}

	public boolean checkBackAny(Type... types) {
		return checkAny(-1, types);
	}

	public boolean checkBackBackAny(Type... types) {
		return checkAny(-2, types);
	}

	public boolean checkSequence(int index, Type... types) {
		if (types == null || types.length == 0)
			return true;
		for (int i = 0; i < types.length; i++) {
			if (!check(index + i, types[i])) {
				return false;
			}
		}
		return true;
	}

	public boolean checkSequence(Type... types) {
		return checkSequence(0, types);
	}

	// Verificações por lexema

	public boolean check(String lexeme) {
		Token t = offset(0);
		return t != null && t.lexeme != null && t.lexeme.equals(lexeme);
	}

	public boolean check(int index, String lexeme) {
		Token t = offset(index);
		return t != null && t.lexeme != null && t.lexeme.equals(lexeme);
	}

	public boolean checkNext(String lexeme) {
		return check(1, lexeme);
	}

	public boolean checkNextNext(String lexeme) {
		return check(2, lexeme);
	}

	public boolean checkBack(String lexeme) {
		return check(-1, lexeme);
	}

	public boolean checkBackBack(String lexeme) {
		return check(-2, lexeme);
	}

	public boolean checkAny(String... lexemes) {
		if (lexemes == null || lexemes.length == 0)
			return false;
		Token t = offset(0);
		if (t == null || t.lexeme == null)
			return false;
		for (String lexeme : lexemes) {
			if (t.lexeme.equals(lexeme))
				return true;
		}
		return false;
	}

	public boolean checkAny(int index, String... lexemes) {
		if (lexemes == null || lexemes.length == 0)
			return false;
		Token t = offset(index);
		if (t == null || t.lexeme == null)
			return false;
		for (String lexeme : lexemes) {
			if (t.lexeme.equals(lexeme))
				return true;
		}
		return false;
	}

	public boolean checkNextAny(String... lexemes) {
		return checkAny(1, lexemes);
	}

	public boolean checkNextNextAny(String... lexemes) {
		return checkAny(2, lexemes);
	}

	public boolean checkBackAny(String... lexemes) {
		return checkAny(-1, lexemes);
	}

	public boolean checkBackBackAny(String... lexemes) {
		return checkAny(-2, lexemes);
	}

	public boolean checkSequence(int index, String... lexemes) {
		if (lexemes == null || lexemes.length == 0)
			return true;
		for (int i = 0; i < lexemes.length; i++) {
			if (!check(index + i, lexemes[i])) {
				return false;
			}
		}
		return true;
	}

	public boolean checkSequence(String... lexemes) {
		return checkSequence(0, lexemes);
	}

	// Match (consumir se corresponder)

	public boolean match(Type type) {
		if (check(type)) {
			next();
			return true;
		}
		return false;
	}

	public boolean match(String lexeme) {
		if (check(lexeme)) {
			next();
			return true;
		}
		return false;
	}

	public boolean matchAny(Type... types) {
		if (types == null || types.length == 0)
			return false;
		for (Type type : types) {
			if (check(type)) {
				next();
				return true;
			}
		}
		return false;
	}

	public boolean matchAny(String... lexemes) {
		if (lexemes == null || lexemes.length == 0)
			return false;
		for (String lexeme : lexemes) {
			if (check(lexeme)) {
				next();
				return true;
			}
		}
		return false;
	}

	public boolean matchSequence(Type... types) {
		if (types == null || types.length == 0)
			return true;
		if (!checkSequence(types)) {
			return false;
		}
		for (int i = 0; i < types.length; i++) {
			next();
		}
		return true;
	}

	public boolean matchSequence(String... lexemes) {
		if (lexemes == null || lexemes.length == 0)
			return true;
		if (!checkSequence(lexemes)) {
			return false;
		}
		for (int i = 0; i < lexemes.length; i++) {
			next();
		}
		return true;
	}

	// Expect (consumir obrigatoriamente)

	public Token expect(Type type) {
		Token token = offset(0);

		if (token == null || token.type != type) {
			throw new IllegalStateException(
					"Expected " + type +
							" but found " +
							(token != null ? token.type : "EOF"));
		}

		next();
		return token;
	}

	public Token expect(String lexeme) {
		Token token = offset(0);

		if (token == null ||
				token.lexeme == null ||
				!token.lexeme.equals(lexeme)) {

			throw new IllegalStateException(
					"Expected \"" + lexeme +
							"\" but found \"" +
							(token != null ? token.lexeme : "EOF") +
							"\"");
		}

		next();
		return token;
	}

	public Token expectAny(Type... types) {
		if (types == null || types.length == 0) {
			throw new IllegalArgumentException("At least one type must be provided");
		}

		Token token = offset(0);

		if (token != null) {
			for (Type type : types) {
				if (token.type == type) {
					next();
					return token;
				}
			}
		}

		throw new IllegalStateException(
				"Unexpected token: " +
						(token != null ? token.type : "EOF"));
	}

	public Token expectAny(String... lexemes) {
		if (lexemes == null || lexemes.length == 0) {
			throw new IllegalArgumentException("At least one lexeme must be provided");
		}

		Token token = offset(0);

		if (token != null && token.lexeme != null) {
			for (String lexeme : lexemes) {
				if (token.lexeme.equals(lexeme)) {
					next();
					return token;
				}
			}
		}

		throw new IllegalStateException(
				"Unexpected token: \"" +
						(token != null ? token.lexeme : "EOF") +
						"\"");
	}

	public Token[] expectSequence(Type... types) {
		if (types == null || types.length == 0) {
			throw new IllegalArgumentException("At least one type must be provided");
		}

		if (!checkSequence(types)) {
			Token token = offset(0);
			throw new IllegalStateException(
					"Expected sequence of types starting with " + types[0] +
							" but found " + (token != null ? token.type : "EOF"));
		}

		Token[] tokens = new Token[types.length];
		for (int i = 0; i < types.length; i++) {
			tokens[i] = next();
		}
		return tokens;
	}

	public Token[] expectSequence(String... lexemes) {
		if (lexemes == null || lexemes.length == 0) {
			throw new IllegalArgumentException("At least one lexeme must be provided");
		}

		if (!checkSequence(lexemes)) {
			Token token = offset(0);
			throw new IllegalStateException(
					"Expected sequence of lexemes starting with \"" + lexemes[0] +
							"\" but found \"" + (token != null ? token.lexeme : "EOF") + "\"");
		}

		Token[] tokens = new Token[lexemes.length];
		for (int i = 0; i < lexemes.length; i++) {
			tokens[i] = next();
		}
		return tokens;
	}

	// Informações do buffer

	public boolean isAtEnd() {
		return !hasNext();
	}

	public int getHead() {
		return super.getHead();
	}

	public int getTail() {
		return super.getTail();
	}

	public int getBufferSize() {
		return super.getBufferSize();
	}

	public void reset() {
		release();
	}
}