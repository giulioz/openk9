package io.openk9.entity.manager.service.index;

import io.openk9.entity.manager.model.index.EntityIndex;
import io.vertx.core.json.JsonObject;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class EntityService {

	public EntityIndex get(long tenantId, long id) throws IOException {

		GetRequest getRequest = new GetRequest(
			tenantId + "-entity", Long.toString(id));

		GetResponse getResponse = _restHighLevelClient.get(
			getRequest, RequestOptions.DEFAULT);

		if (getResponse.isExists()) {
			String sourceAsString = getResponse.getSourceAsString();
			JsonObject json = new JsonObject(sourceAsString);
			return json.mapTo(EntityIndex.class);
		}
		return null;
	}

	public void index(EntityIndex entity) throws IOException {

		IndexRequest request =
			new IndexRequest(entity.getTenantId() + "-entity");

		request.id(Long.toString(entity.getId()));

		Map<String, Object> json = new HashMap<>();

		json.put("name", entity.getName());
		json.put("tenantId", entity.getTenantId());
		json.put("id", entity.getId());
		json.put("type", entity.getType());

		request.source(json, XContentType.JSON);

		_indexerBus.emit(request);

	}

	public EntityIndex searchByNameAndType(
		long tenantId, String name, String type) throws IOException {

		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

		boolQueryBuilder.must(QueryBuilders.matchQuery("name", name));
		boolQueryBuilder.must(QueryBuilders.matchQuery("type", type));

		List<EntityIndex> search = search(tenantId, boolQueryBuilder, 0, 1);

		if (search.isEmpty()) {
			return null;
		}

		return search.get(0);
	}

	public List<EntityIndex> search(long tenantId, String term, String match) throws IOException {
		return search(tenantId, QueryBuilders.matchQuery(term, match), 0, 20);
	}

	public List<EntityIndex> search(long tenantId, QueryBuilder queryBuilder, int from, int size) throws IOException {

		SearchRequest searchRequest = new SearchRequest(tenantId + "-entity");
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(queryBuilder);
		searchSourceBuilder.size(from);
		searchSourceBuilder.size(size);
		searchRequest.source(searchSourceBuilder);

		SearchResponse searchResponse = _restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
		SearchHits hits = searchResponse.getHits();
		List<EntityIndex> results = new ArrayList<>(hits.getHits().length);
		for (SearchHit hit : hits.getHits()) {
			String sourceAsString = hit.getSourceAsString();
			JsonObject json = new JsonObject(sourceAsString);
			EntityIndex entityIndex = json.mapTo(EntityIndex.class);
			entityIndex.setScore(hit.getScore());
			results.add(entityIndex);
		}
		return results;

	}

	@Inject
	RestHighLevelClient _restHighLevelClient;

	@Inject
	IndexerBus _indexerBus;

}
