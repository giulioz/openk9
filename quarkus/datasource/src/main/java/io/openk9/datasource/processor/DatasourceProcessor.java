package io.openk9.datasource.processor;


import io.openk9.datasource.model.Datasource;
import io.openk9.datasource.model.EnrichItem;
import io.openk9.datasource.model.EnrichPipeline;
import io.openk9.datasource.model.Tenant;
import io.openk9.datasource.processor.payload.DatasourceContext;
import io.openk9.datasource.processor.payload.IngestionDatasourcePayload;
import io.openk9.datasource.processor.payload.IngestionPayload;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class DatasourceProcessor {

	@Incoming("ingestion")
	@Outgoing("ingestion-datasource")
	public Uni<IngestionDatasourcePayload> process(JsonObject jsonObject) throws Exception {

		long datasourceId = jsonObject.getLong("datasourceId");

		Uni<Datasource> datasourceUni =
			Datasource.findById(datasourceId);

		return Panache.withTransaction(() ->
			datasourceUni
				.flatMap(datasource ->
					EnrichPipeline
						.findByDatasourceId(datasource.getDatasourceId())
						.onItem()
						.ifNull()
						.continueWith(EnrichPipeline::new)
						.flatMap(enrichPipeline -> {

							Uni<List<EnrichItem>> enrichItemUni;

							if (enrichPipeline.getEnrichPipelineId() != null) {

								enrichItemUni = EnrichItem
									.findByEnrichPipelineId(
										enrichPipeline.getEnrichPipelineId())
									.onItem()
									.ifNull()
									.continueWith(List::of);

							}
							else {
								enrichItemUni = Uni.createFrom().item(List.of());
							}

							return Uni
								.combine()
								.all()
								.unis(
									Tenant.findById(datasource.getTenantId()),
									enrichItemUni)
								.combinedWith((tenantObj, enrichItemList) -> {

									Tenant tenant = (Tenant)tenantObj;

									IngestionPayload ingestionPayload =
										jsonObject.mapTo(IngestionPayload.class);

									ingestionPayload.setTenantId(tenant.getTenantId());

									DatasourceContext datasourceContext = DatasourceContext.of(
										datasource, tenant, enrichPipeline, enrichItemList
									);

									return IngestionDatasourcePayload.of(
										ingestionPayload, datasourceContext);
								});

						}))
				.eventually(() -> Datasource
					.<Datasource>findById(datasourceId)
					.flatMap(datasource -> {

						datasource.setLastIngestionDate(
							Instant.ofEpochMilli(
								jsonObject.getLong("parsingDate")));

						return datasource.persist();

					})
				)
		);

	}

}
