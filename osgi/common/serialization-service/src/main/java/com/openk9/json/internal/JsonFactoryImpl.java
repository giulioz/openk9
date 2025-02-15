/*
 * Copyright (c) 2020-present SMC Treviso s.r.l. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.openk9.json.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.openk9.json.api.ArrayNode;
import com.openk9.json.api.JsonFactory;
import com.openk9.json.api.JsonNode;
import com.openk9.json.api.ObjectNode;
import com.openk9.json.exception.JsonException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component(immediate = true, service = JsonFactory.class)
public class JsonFactoryImpl implements JsonFactory {

	@Override
	public String toJson(Object o) {
		try {
			return _objectMapperProvider
				.getObjectMapper().writeValueAsString(o);
		}
		catch (JsonProcessingException e) {
			throw new com.openk9.json.exception.JsonProcessingException(
				e);
		}
	}

	@Override
	public String toPrettyJson(Object o) {
		try {
			return _objectMapperProvider.getObjectMapper()
				.writerWithDefaultPrettyPrinter()
				.writeValueAsString(o);
		}
		catch (JsonProcessingException e) {
			throw new com.openk9.json.exception.JsonProcessingException(
				e);
		}
	}

	@Override
	public <T> T fromJson(String json, Class<T> clazz) {
		try {
			return _objectMapperProvider
				.getObjectMapper().readValue(json, clazz);
		}
		catch (IOException exception) {
			throw new JsonException(exception);
		}
	}

	@Override
	public <T> List<T> fromJsonList(String jsonList, Class<T> clazz) {
		try {
			return _objectMapperProvider
				.getObjectMapper().readerForListOf(clazz).readValue(jsonList);
		}
		catch (IOException exception) {
			throw new JsonException(exception);
		}
	}

	@Override
	public <T> T[] fromJsonArray(String jsonList, Class<T> clazz) {
		try {
			return _objectMapperProvider
				.getObjectMapper().readerForArrayOf(clazz).readValue(jsonList);
		}
		catch (IOException exception) {
			throw new JsonException(exception);
		}
	}

	@Override
	public <T> Map<String, T> fromJsonMap(String jsonList, Class<T> clazz) {
		try {
			return _objectMapperProvider
				.getObjectMapper().readerForMapOf(clazz).readValue(jsonList);
		}
		catch (IOException exception) {
			throw new JsonException(exception);
		}
	}

	@Override
	public JsonNode fromJsonToJsonNode(String json) {
		try {
			return new JsonNodeWrapper(
				_objectMapperProvider.getObjectMapper().readTree(json));
		}
		catch (JsonProcessingException e) {
			throw new JsonException(e);
		}
	}

	@Override
	public JsonNode fromJsonToJsonNode(byte[] json) {
		try {
			return new JsonNodeWrapper(
				_objectMapperProvider.getObjectMapper().readTree(json));
		}
		catch (IOException e) {
			throw new JsonException(e);
		}
	}

	@Override
	public Object toJsonClassDefinition(Class<?> clazz) {

		ObjectMapper mapper = _objectMapperProvider.getObjectMapper();

		SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();

		try {

			mapper.acceptJsonFormatVisitor(clazz, visitor);

			return visitor.finalSchema();
		}
		catch (Exception e) {
			throw new JsonException(e);
		}

	}

	@Override
	public ObjectNode createObjectNode() {
		return new ObjectNodeWrapper(
			_objectMapperProvider.getObjectMapper().createObjectNode());
	}

	@Override
	public ArrayNode createArrayNode() {
		return new ArrayNodeWrapper(
			_objectMapperProvider.getObjectMapper().createArrayNode());
	}

	@Reference
	private ObjectMapperProvider _objectMapperProvider;

}
