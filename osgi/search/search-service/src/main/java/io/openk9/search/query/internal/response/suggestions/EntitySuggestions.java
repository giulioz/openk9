package io.openk9.search.query.internal.response.suggestions;

class EntitySuggestions extends Suggestions {

	EntitySuggestions(
		String value, String entityType, String entityValue) {
		super(TokenType.ENTITY, value);
		this.entityType = entityType;
		this.entityValue = entityValue;
	}

	public String getEntityType() {
		return entityType;
	}

	public String getEntityValue() {
		return entityValue;
	}

	private final String entityType;
	private final String entityValue;

}
