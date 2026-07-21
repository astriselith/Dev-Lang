package com.lang.json;

import com.lang.ast.Comment;
import com.lang.token.Token;
import com.lang.token.TokenStream;
import com.lang.unit.CompilationUnit;

import java.util.*;

import static com.lang.json.JsonErrorCode.*;
import static com.lang.token.Type.*;

public class JsonParser {
	private final CompilationUnit unit;
	private TokenStream stream;

	public JsonParser(TokenStream stream, CompilationUnit unit) {
		this.stream = stream;
		this.unit = unit != null ? unit : new CompilationUnit();

		if (stream == null) {
			throw new IllegalArgumentException("TokenStream cannot be null");
		}

		this.stream.setHandler((token) -> {
			if (token.isComment()) {
				this.unit.addComment(new Comment(token.lexeme, token.position));
				return false; // Skip comments
			}
			return true; // Process other tokens
		});
	}

	public Json parse() {
		return value();
	}

	private Json value() {
		Token token = stream.peek();

		if (token.typeEquals(NULL)) {
			stream.advance();
			return new JsonNull(token.getPosition());
		}

		if (token.typeEquals(BOOL)) {
			stream.advance();
			return new JsonBoolean(Boolean.parseBoolean(token.lexeme), token.getPosition());
		}

		if (token.typeEquals(INT)) {
			stream.advance();
			return new JsonNumber(Long.parseLong(token.lexeme), token.getPosition());
		}

		if (token.typeEquals(FLOAT)) {
			stream.advance();
			return new JsonNumber(Double.parseDouble(token.lexeme), token.getPosition());
		}

		if (token.typeEquals(STRING)) {
			stream.advance();
			return new JsonString(token.lexeme, token.getPosition());
		}

		if (token.typeEquals(LBRACE)) {
			return object();
		}

		if (token.typeEquals(LBRACKET)) {
			return array();
		}

		throw unit.error(TAG, UNEXPECTED_TOKEN.format(token.lexeme), token);
	}

	private JsonObject object() {
		Token start = stream.advance();
		Map<String, Json> fields = new LinkedHashMap<>();

		if (stream.check(RBRACE)) {
			stream.advance();
			return new JsonObject(fields, start.getPosition());
		}

		do {
			Token keyToken = stream.peek();

			if (!stream.match(STRING)) {
				throw unit.error(TAG, EXPECTED_STRING_KEY.getMessage(), keyToken);
			}

			String key = (String) keyToken.lexeme;

			if (!stream.match(COLON)) {
				throw unit.error(TAG, EXPECTED_COLON_AFTER_KEY.getMessage(), stream.previous());
			}

			fields.put(key, value());

		} while (stream.match(COMMA));

		if (!stream.check(RBRACE)) {
			throw unit.error(TAG, EXPECTED_RBRACE.getMessage(), stream.peek());
		}
		stream.advance();

		return new JsonObject(fields, start.getPosition());
	}

	private JsonArray array() {
		Token start = stream.advance();
		List<Json> elements = new ArrayList<>();

		if (stream.check(RBRACKET)) {
			stream.advance();
			return new JsonArray(elements, start.getPosition());
		}

		do {
			elements.add(value());
		} while (stream.match(COMMA));

		if (!stream.check(RBRACKET)) {
			throw unit.error(TAG, EXPECTED_RBRACKET.getMessage(), stream.peek());
		}
		stream.advance();

		return new JsonArray(elements, start.getPosition());
	}

	public CompilationUnit getCompilationUnit() {
		return unit;
	}
}