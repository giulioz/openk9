package io.openk9.search.query.internal.response.suggestions;

import java.util.Objects;

public abstract class Suggestions {

	Suggestions(TokenType tokenType, String value) {
		this.tokenType = tokenType;
		this.value = value;
	}

	public TokenType getTokenType() {
		return tokenType;
	}

	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Suggestions)) {
			return false;
		}
		Suggestions that = (Suggestions) o;
		return tokenType == that.tokenType && value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tokenType, value);
	}

	private final TokenType tokenType;
	private final String value;

	public static Suggestions entity(
		String value, String entityType, String entityValue) {

		return new EntitySuggestions(value, entityType, entityValue);
	}

	public static Suggestions entity(
		String value, String entityType, String entityValue,
		String keywordKey) {

		return new EntityContextSuggestions(
			value, entityType, entityValue, keywordKey);
	}

	public static Suggestions text(String value, String keywordKey) {
		return new TextSuggestions(value, keywordKey);
	}

	public static Suggestions docType(String value) {
		return new DocTypeSuggestions(value);
	}

	public static Suggestions datasource(String value) {
		return new DatasourceSuggestions(value);
	}

}
