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

package com.openk9.datasource.internal.web;

import com.openk9.datasource.model.EnrichItem;
import com.openk9.datasource.repository.EnrichItemRepository;
import com.openk9.http.util.BaseEndpointRegister;
import com.openk9.http.web.HttpHandler;
import com.openk9.http.web.HttpRequest;
import com.openk9.http.web.HttpResponse;
import com.openk9.json.api.JsonFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component(immediate = true, service = EnrichItemEndpoints.class)
public class EnrichItemEndpoints extends BaseEndpointRegister {

	@Activate
	public void activate(BundleContext bundleContext) {
		setBundleContext(bundleContext);

		this.registerEndpoint(
			HttpHandler.post("/", this::_addDatasource),
			HttpHandler.delete("/{id}", this::_deleteDatasource),
			HttpHandler.get("/{id}", this::_getDatasourceById),
			HttpHandler.get("/", this::_findAll),
			HttpHandler.put("/", this::_updateDatasource)
		);

	}

	@Deactivate
	public void deactivate() {
		this.close();
	}

	private Publisher<Void> _findAll(
		HttpRequest httpRequest, HttpResponse httpResponse) {

		Flux<String> response =
			_enrichItemRepository
				.findAll()
				.map(_jsonFactory::toJson);

		return httpResponse.sendString(response);
	}

	private Publisher<Void> _updateDatasource(
		HttpRequest httpRequest, HttpResponse httpResponse) {

		Mono<String> jsonResponse =
			_getDatasourceFromBodyAttribute(httpRequest)
				.flatMap(_enrichItemRepository::updateEnrichItem)
				.map(_jsonFactory::toJson);

		return httpResponse.sendString(jsonResponse);
	}

	private Publisher<Void> _getDatasourceById(
		HttpRequest httpRequest, HttpResponse httpResponse) {
		String id = httpRequest.pathParam("id");

		Mono<String> response = _enrichItemRepository
			.findByPrimaryKey(Long.valueOf(id))
			.map(_jsonFactory::toJson);

		return httpResponse.sendString(response);
	}

	private Publisher<Void> _deleteDatasource(
		HttpRequest httpRequest, HttpResponse httpResponse) {

		String id = httpRequest.pathParam("id");

		return _enrichItemRepository
			.removeEnrichItem(Long.valueOf(id))
			.then((Mono<Void>)httpResponse.sendString(Mono.just("{}")));
	}

	private Publisher<Void> _addDatasource(
		HttpRequest request, HttpResponse response) {

		Mono<String> jsonResponse =
			_getDatasourceFromBodyAttribute(request)
				.flatMap(_enrichItemRepository::addEnrichItem)
				.map(_jsonFactory::toJson);

		return response.sendString(jsonResponse);

	}

	private Mono<EnrichItem> _getDatasourceFromBodyAttribute(
		HttpRequest request) {

		return Mono
			.from(request.aggregateBodyToString())
			.map(s -> _jsonFactory.fromJson(s, EnrichItem.class));

	}

	@Override
	public String getBasePath() {
		return "/v1/enrich-item";
	}

	@Reference
	private JsonFactory _jsonFactory;

	@Reference
	private EnrichItemRepository _enrichItemRepository;
}
