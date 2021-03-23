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

package io.openk9.datasource.internal.web;

import io.openk9.http.util.HttpResponseWriter;
import io.openk9.http.web.Endpoint;
import io.openk9.http.web.HttpHandler;
import io.openk9.http.web.HttpRequest;
import io.openk9.http.web.HttpResponse;
import io.openk9.ingestion.driver.manager.api.PluginDriver;
import io.openk9.ingestion.driver.manager.api.PluginDriverRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.reactivestreams.Publisher;

import java.util.stream.Collectors;

@Component(
	immediate = true,
	service = Endpoint.class
)
public class PluginDriverEndpoint implements HttpHandler {

	@Override
	public String getPath() {
		return "/v1/driver-service-names";
	}

	@Override
	public Publisher<Void> apply(
		HttpRequest httpRequest, HttpResponse httpResponse) {


		return _httpResponseWriter.write(
			httpResponse,
			_pluginDriverRegistry
				.getPluginDriverList()
				.stream()
				.map(PluginDriver::getName)
				.collect(Collectors.toList())
		);

	}

	@Reference
	private HttpResponseWriter _httpResponseWriter;

	@Reference
	private PluginDriverRegistry _pluginDriverRegistry;

}
