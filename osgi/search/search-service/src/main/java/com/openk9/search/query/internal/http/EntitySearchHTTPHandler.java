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

package com.openk9.search.query.internal.http;

import com.openk9.datasource.model.Tenant;
import com.openk9.datasource.repository.TenantRepository;
import com.openk9.http.util.HttpResponseWriter;
import com.openk9.http.util.HttpUtil;
import com.openk9.http.web.Endpoint;
import com.openk9.http.web.HttpHandler;
import com.openk9.http.web.HttpRequest;
import com.openk9.http.web.HttpResponse;
import com.openk9.json.api.JsonFactory;
import com.openk9.json.api.JsonNode;
import com.openk9.json.api.ObjectNode;
import com.openk9.search.client.api.Search;
import com.openk9.search.client.api.SearchRequestFactory;
import com.openk9.search.enrich.mapper.api.EntityMapper;
import com.openk9.search.enrich.mapper.api.EntityMapperProvider;
import com.openk9.search.query.internal.response.Response;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(
	immediate = true,
	service = Endpoint.class,
	property = {
		"base.path=/v1/entity"
	}
)
public class EntitySearchHTTPHandler implements HttpHandler {

	@Override
	public String getPath() {
		return "";
	}

	@Override
	public int method() {
		return HttpHandler.GET + HttpHandler.POST;
	}

	@Override
	public Publisher<Void> apply(
		HttpRequest httpRequest, HttpResponse httpResponse) {

		String hostName = HttpUtil.getHostName(httpRequest);

		Mono<Response> response =
			_tenantRepository
				.findByVirtualHost(hostName)
				.switchIfEmpty(
					Mono.error(
						() -> new RuntimeException(
							"tenant not found for virtualhost: " + hostName)))
				.map(Tenant::getTenantId)
				.flatMap(tenantId -> Mono
					.from(httpRequest.aggregateBodyToByteArray())
					.map(_jsonFactory::fromJsonToJsonNode)
					.map(JsonNode::toObjectNode)
					.map(jsonObj -> _toSearchRequest(tenantId, jsonObj))
				)
				.flatMap(_search::search)
				.map(SearchResponse::getHits)
				.map(this::_searchHitToResponse);

		return _httpResponseWriter.write(httpResponse, response);

	}

	private Response _searchHitToResponse(SearchHits searchHits) {

		SearchHit[] hits = searchHits.getHits();

		List<Map<String, Object>> result = new ArrayList<>(hits.length);

		for (SearchHit hit : hits) {

			Map<String, Object> sourceMap =
				new HashMap<>(hit.getSourceAsMap());

			sourceMap.put("entityId", hit.getId());

			result.add(sourceMap);

		}

		TotalHits totalHits = searchHits.getTotalHits();

		return new Response(
			result,
			totalHits.value,
			totalHits.relation == TotalHits.Relation.EQUAL_TO
		);
	}

	private SearchRequest _toSearchRequest(
		Long tenantId, ObjectNode jsonNodes) {

		BoolQueryBuilder boolQuery = _getBoolQueryBuilder(jsonNodes);

		SearchRequest searchRequest =
			_searchRequestFactory.createSearchRequestEntity(tenantId);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		searchSourceBuilder.query(boolQuery);

		return searchRequest.source(searchSourceBuilder);
	}

	private BoolQueryBuilder _getBoolQueryBuilder(ObjectNode jsonNodes) {

		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

		for (Map.Entry<String, JsonNode> jsonField : jsonNodes.fields()) {

			String key = jsonField.getKey();
			String value = jsonField.getValue().asText();

			switch (key) {
				case ENTITY_ID: {
					boolQuery.must(QueryBuilders.idsQuery().addIds(value));
					break;
				}
				case ALL: {

					BoolQueryBuilder allQuery = QueryBuilders.boolQuery();

					_entityMapperProvider
						.getEntityMappers()
						.stream()
						.map(EntityMapper::getSearchKeywords)
						.flatMap(Arrays::stream)
						.distinct()
						.map(field -> QueryBuilders.matchBoolPrefixQuery(
							field, value.toLowerCase()))
						.forEach(allQuery::should);

					boolQuery.must(allQuery);

					break;
				}
				default: {
					boolQuery.must(
						QueryBuilders.matchBoolPrefixQuery(
							key, value.toLowerCase()));
				}

			}

		}
		return boolQuery;
	}

	public static final String ENTITY_ID = "entityId";

	public static final String ALL = "all";

	@Reference
	private JsonFactory _jsonFactory;

	@Reference
	private HttpResponseWriter _httpResponseWriter;

	@Reference
	private TenantRepository _tenantRepository;

	@Reference
	private SearchRequestFactory _searchRequestFactory;

	@Reference
	private EntityMapperProvider _entityMapperProvider;

	@Reference
	private Search _search;

}
