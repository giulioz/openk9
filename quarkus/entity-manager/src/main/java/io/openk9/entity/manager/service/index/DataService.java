package io.openk9.entity.manager.service.index;

import io.openk9.entity.manager.cache.model.IngestionEntity;
import io.openk9.entity.manager.model.index.DataEntityIndex;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class DataService {

	public boolean associateEntity(
		long tenantId, String ingestionId, IngestionEntity entity)
		throws IOException {

		MatchQueryBuilder query =
			QueryBuilders.matchQuery("ingestionId", ingestionId);

		SearchResponse searchResponse = search(tenantId, query);

		for (SearchHit hit : searchResponse.getHits()) {
			String indexName = hit.getIndex();
			String id = hit.getId();
			JsonObject dataDocument = new JsonObject(hit.getSourceAsString());

			JsonArray entities = dataDocument.getJsonArray("entities");

			if (entities == null) {
				entities = new JsonArray();
			}

			entities.add(JsonObject.mapFrom(
				DataEntityIndex.of(
					entity.getId(), entity.getType(), entity.getContext())
			));

			dataDocument.put("entities", entities);

			UpdateRequest updateRequest = new UpdateRequest(indexName, id);

			updateRequest.doc(dataDocument.toString(), XContentType.JSON);

			_indexerBus.emit(updateRequest);

			return true;

		}

		return false;

	}

	public boolean associateEntities(
			long tenantId, String ingestionId,
			List<IngestionEntity> ingestionEntities)
		throws IOException {

		MatchQueryBuilder query =
			QueryBuilders.matchQuery("ingestionId", ingestionId);

		SearchResponse searchResponse = search(tenantId, query);

		for (SearchHit hit : searchResponse.getHits()) {
			String indexName = hit.getIndex();
			String id = hit.getId();
			JsonObject dataDocument = new JsonObject(hit.getSourceAsString());

			JsonArray entities = dataDocument.getJsonArray("entities");

			if (entities == null) {
				entities = new JsonArray();
			}

			ingestionEntities
				.stream()
				.map(entity -> DataEntityIndex.of(
					entity.getId(), entity.getType(), entity.getContext()))
				.map(JsonObject::mapFrom)
				.forEach(entities::add);

			dataDocument.put("entities", entities);

			UpdateRequest updateRequest = new UpdateRequest(indexName, id);

			updateRequest.doc(dataDocument.toString(), XContentType.JSON);

			updateRequest.setRefreshPolicy(
				WriteRequest.RefreshPolicy.WAIT_UNTIL);

			UpdateResponse update =
				_restHighLevelClient.update(
					updateRequest,
					RequestOptions.DEFAULT);

			if (_logger.isDebugEnabled()) {
				_logger.debug(update.toString());
			}

			return true;

		}

		return false;

	}

	private SearchResponse search(long tenantId, String term, String match) throws IOException {
		return search(tenantId, QueryBuilders.matchQuery(term, match));
	}

	private SearchResponse search(long tenantId, QueryBuilder queryBuilder) throws IOException {

		SearchRequest searchRequest = new SearchRequest(tenantId + "-*-data");
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(queryBuilder);
		searchRequest.source(searchSourceBuilder);

		return _restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

	}

	@Inject
	RestHighLevelClient _restHighLevelClient;

	@Inject
	IndexerBus _indexerBus;

	@Inject
	Logger _logger;

}
