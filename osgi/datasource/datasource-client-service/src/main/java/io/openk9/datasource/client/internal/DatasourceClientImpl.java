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

package io.openk9.datasource.client.internal;

import io.openk9.datasource.client.api.DatasourceClient;
import io.openk9.http.client.HttpClient;
import io.openk9.http.client.HttpClientFactory;
import io.openk9.http.web.HttpHandler;
import io.openk9.json.api.JsonFactory;
import io.openk9.model.Datasource;
import io.openk9.model.SuggestionCategory;
import io.openk9.model.SuggestionCategoryField;
import io.openk9.model.SuggestionCategoryPayload;
import io.openk9.model.Tenant;
import io.openk9.model.TenantDatasource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component(
	immediate = true,
	service = DatasourceClient.class
)
public class DatasourceClientImpl implements DatasourceClient {

	@interface Config {
		String baseUrl() default "http://openk9-datasource:8080/";
	}

	@Activate
	public void activate(Config config) {
		_httpClient = _httpClientFactory.getHttpClient(config.baseUrl());
	}

	@Modified
	public void modified(Config config) {
		activate(config);
	}

	@Override
	public Mono<TenantDatasource> findTenantAndDatasourceByDatasourceId(
		long datasourceId) {
		return findDatasource(datasourceId)
			.zipWhen(
				datasource -> findTenant(datasource.getTenantId()),
				TenantDatasource::of
			);
	}

	@Override
	public Mono<Datasource> findDatasource(long datasourceId) {
		return Mono.from(
				_httpClient
					.request(
						HttpHandler.GET,
						"/v2/datasource/" + datasourceId
					)
			)
			.map(response -> _jsonFactory.fromJson(response, Datasource.class));
	}

	@Override
	public Flux<Datasource> findDatasourceByTenantIdAndIsActive(long tenantId) {
		return Mono.from(
				_httpClient
					.request(
						HttpHandler.POST,
						"/v2/datasource/filter",
						_jsonFactory
							.createObjectNode()
							.put("tenantId", tenantId)
							.put("active", true)
							.toString(),
						Map.of(
							"Content-Type", "application/json"
						)
					)
			)
			.flatMapIterable(
				response -> _jsonFactory.fromJsonList(
					response, Datasource.class));
	}

	@Override
	public Flux<Datasource> findDatasourceByTenantId(long tenantId) {
		return Mono.from(
				_httpClient
					.request(
						HttpHandler.POST,
						"/v2/datasource/filter",
						_jsonFactory
							.createObjectNode()
							.put("tenantId", tenantId)
							.toString(),
						Map.of(
							"Content-Type", "application/json"
						)
					)
			)
			.flatMapIterable(
				response -> _jsonFactory.fromJsonList(
					response, Datasource.class));
	}

	@Override
	public Flux<Tenant> findTenantByVirtualHost(String virtualHost) {
		return Mono.from(
				_httpClient
					.request(
						HttpHandler.POST,
						"/v2/tenant/filter",
						_jsonFactory
							.createObjectNode()
							.put("virtualHost", virtualHost)
							.toString(),
						Map.of(
							"Content-Type", "application/json"
						)
					)
			)
			.flatMapIterable(response -> _jsonFactory.fromJsonList(response, Tenant.class));
	}

	@Override
	public Mono<Tenant> findTenant(long tenantId) {
		return Mono.from(
				_httpClient
					.request(
						HttpHandler.GET,
						"/v2/tenant/" + tenantId
					)
			)
			.map(response -> _jsonFactory.fromJson(response, Tenant.class));
	}

	@Override
	public Mono<List<SuggestionCategory>> findSuggestionCategories() {
		return Mono.from(
				_httpClient
					.request(
						HttpHandler.GET,
						"/v2/suggestion-category"
					)
			)
			.map(response -> _jsonFactory.fromJsonList(
				response, SuggestionCategory.class));
	}

	@Override
	public Mono<List<SuggestionCategory>> findSuggestionCategories(
		long tenantId) {
		return Mono.from(
				_httpClient
					.request(
						HttpHandler.POST,
						"/v2/suggestion-category/filter",
						_jsonFactory
							.createObjectNode()
							.put("tenantId", tenantId)
							.put("enabled", true)
							.toString(),
						Map.of(
							"Content-Type", "application/json"
						)
					)
			)
			.map(response ->
				_jsonFactory.fromJsonList(response, SuggestionCategory.class));
	}

	@Override
	public Mono<SuggestionCategory> findSuggestionCategory(long categoryId) {
		return Mono.from(
				_httpClient
					.request(
						HttpHandler.GET,
						"/v2/suggestion-category/" + categoryId
					)
			)
			.map(response -> _jsonFactory.fromJson(response, SuggestionCategory.class));
	}

	@Override
	public Mono<List<SuggestionCategory>> findSuggestionCategoryByTenantIdAndCategoryId(
		long tenantId, long categoryId) {

		return Mono.from(
				_httpClient
					.request(
						HttpHandler.POST,
						"/v2/suggestion-category/filter",
						_jsonFactory
							.createObjectNode()
							.put("suggestionCategoryId", categoryId)
							.put("tenantId", tenantId)
							.put("enabled", true)
							.toString(),
						Map.of(
							"Content-Type", "application/json"
						)
					)
			)
			.map(response ->
				_jsonFactory.fromJsonList(response, SuggestionCategory.class));
	}

	@Override
	public Mono<List<SuggestionCategoryPayload>> findSuggestionCategoriesWithFields() {
		return findSuggestionCategories()
			.flatMapIterable(Function.identity())
			.flatMap(suggestionCategory ->
				findSuggestionCategoryFieldsByCategoryId(
					suggestionCategory.getSuggestionCategoryId())
					.map(fields -> SuggestionCategoryPayload.of(
						suggestionCategory.getSuggestionCategoryId(),
						suggestionCategory.getTenantId(),
						suggestionCategory.getParentCategoryId(),
						suggestionCategory.getName(), fields)))
			.collectList();
	}

	@Override
	public Mono<SuggestionCategoryPayload> findSuggestionCategoryWithFieldsById(
		long categoryId) {
		return Mono.zip(
			findSuggestionCategory(categoryId),
			findSuggestionCategoryFieldsByCategoryId(categoryId),
			(suggestionCategory, fields) ->
				SuggestionCategoryPayload.of(
					suggestionCategory.getSuggestionCategoryId(),
					suggestionCategory.getTenantId(),
					suggestionCategory.getParentCategoryId(),
					suggestionCategory.getName(), fields)
		);
	}

	@Override
	public Mono<List<SuggestionCategoryField>> findSuggestionCategoryFields() {
		return Mono.from(
				_httpClient
					.request(
						HttpHandler.GET,
						"/v2/suggestion-category-field"
					)
			)
			.map(response -> _jsonFactory.fromJsonList(
				response, SuggestionCategoryField.class));
	}

	@Override
	public Mono<List<SuggestionCategoryField>> findSuggestionCategoryFieldsByTenantId(
		long tenantId) {
		return Mono.from(
				_httpClient
					.request(
						HttpHandler.POST,
						"/v2/suggestion-category-field/filter",
						_jsonFactory
							.createObjectNode()
							.put("tenantId", tenantId)
							.put("enabled", true)
							.toString(),
						Map.of(
							"Content-Type", "application/json"
						)
					)
			)
			.map(response -> _jsonFactory.fromJsonList(
				response, SuggestionCategoryField.class));
	}

	@Override
	public Mono<List<SuggestionCategoryField>> findSuggestionCategoryFieldsByTenantIdAndCategoryId(
		long tenantId, long categoryId) {
		return Mono.from(
				_httpClient
					.request(
						HttpHandler.POST,
						"/v2/suggestion-category-field/filter",
						_jsonFactory
							.createObjectNode()
							.put("tenantId", tenantId)
							.put("categoryId", categoryId)
							.put("enabled", true)
							.toString(),
						Map.of(
							"Content-Type", "application/json"
						)
					)
			)
			.map(response -> _jsonFactory.fromJsonList(
				response, SuggestionCategoryField.class));
	}

	@Override
	public Mono<List<SuggestionCategoryField>> findSuggestionCategoryFieldsByCategoryId(
		long categoryId) {
		return Mono.from(
				_httpClient
					.request(
						HttpHandler.POST,
						"/v2/suggestion-category-field/filter",
						_jsonFactory
							.createObjectNode()
							.put("categoryId", categoryId)
							.toString(),
						Map.of(
							"Content-Type", "application/json"
						)
					)
			)
			.map(response -> _jsonFactory.fromJsonList(
				response, SuggestionCategoryField.class));
	}

	@Override
	public Mono<List<SuggestionCategoryField>> findSuggestionCategoryFieldsByCategoryIdEnabled(
		long categoryId) {
		return Mono.from(
				_httpClient
					.request(
						HttpHandler.POST,
						"/v2/suggestion-category-field/filter",
						_jsonFactory
							.createObjectNode()
							.put("categoryId", categoryId)
							.put("enabled", true)
							.toString(),
						Map.of(
							"Content-Type", "application/json"
						)
					)
			)
			.map(response -> _jsonFactory.fromJsonList(
				response, SuggestionCategoryField.class));
	}

	private HttpClient _httpClient;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private JsonFactory _jsonFactory;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private HttpClientFactory _httpClientFactory;

}