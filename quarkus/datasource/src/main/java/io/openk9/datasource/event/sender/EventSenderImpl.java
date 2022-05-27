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

package io.openk9.datasource.event.sender;

import io.openk9.datasource.model.Event;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.eventbus.EventBus;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class EventSenderImpl implements EventSender {

	@Override
	public void sendEventAsJson(String type, Object data) {
		sendEventAsJson(type, null, null, data);
	}

	@Override
	public void sendEventAsJson(String type, String groupKey, Object data) {
		sendEventAsJson(type, groupKey, null, data);
	}

	@Override
	public void sendEventAsJson(
		String type, String groupKey, String className, Object data) {

		bus.requestAndForget(
			REGISTER_EVENT,
			EventMessage.of(type, groupKey, className, data)
		);

	}

	@ConsumeEvent(value = REGISTER_EVENT)
	@ReactiveTransactional
	public Uni<Void> handleEvent(EventMessage eventMessage) {

		Object objData = eventMessage.getData();

		String data = objData instanceof String
			? (String)objData
			: Json.encode(objData);

		return Event
			.builder()
			.data(data)
			.size(data == null ? 0 : data.length())
			.groupKey(eventMessage.getGroupKey())
			.type(eventMessage.getType())
			.className(eventMessage.getClassName())
			.build()
			.persist()
			.replaceWithVoid();
	}

	@Inject
	EventBus bus;

	public static final String REGISTER_EVENT = "register.event";

}
