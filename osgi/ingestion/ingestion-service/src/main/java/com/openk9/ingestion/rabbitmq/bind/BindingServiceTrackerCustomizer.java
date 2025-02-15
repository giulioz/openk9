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

package com.openk9.ingestion.rabbitmq.bind;

import com.rabbitmq.client.AMQP;
import com.openk9.ingestion.api.Binding;
import com.openk9.ingestion.api.BundleReceiver;
import com.openk9.ingestion.api.BundleSender;
import com.openk9.ingestion.api.ReceiverReactor;
import com.openk9.osgi.util.AutoCloseables;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.BindingSpecification;
import reactor.rabbitmq.ExchangeSpecification;
import reactor.rabbitmq.QueueSpecification;
import reactor.rabbitmq.Sender;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BindingServiceTrackerCustomizer
	implements ServiceTrackerCustomizer<Binding, Binding> {

	public BindingServiceTrackerCustomizer(
		Sender sender,
		ReceiverReactor receiverReactor,
		BundleContext bundleContext) {
		_sender = sender;
		_receiverReactor = receiverReactor;
		_bundleContext = bundleContext;
	}

	@Override
	public Binding addingService(
		ServiceReference<Binding> reference) {

		Binding service = _bundleContext.getService(reference);

		Bundle bundle = reference.getBundle();

		Binding.Exchange exchangeDTO = service.getExchange();

		String exchange = exchangeDTO.getName();

		String routingKey = service.getRoutingKey();

		String queue = service.getQueue();

		Binding.Exchange.Type exchangeType = exchangeDTO.getType();

		BindingSpecification binding =
			BindingSpecification.binding(exchange, routingKey, queue);

		Mono<AMQP.Exchange.DeclareOk> mono1 = _sender.declareExchange(
			ExchangeSpecification.exchange(exchange).type(exchangeType.name())
		);

		Mono<AMQP.Queue.DeclareOk> mono2 = _sender.declareQueue(
			QueueSpecification.queue(queue));

		Mono<AMQP.Queue.BindOk> mono3 = _sender.bind(binding);

		Mono.zip(mono1, mono2, mono3).block();

		_log.info(
			String.format(
				"Bundle: %s, Service: %s, exchange: %s, exchange type: %s, " +
				"routingKey: %s, queue: %s",
				bundle.getSymbolicName(), service.getClass().getName(),
				exchange, exchangeType.name(), routingKey, queue));

		Dictionary<String, Object> senderProps = new Hashtable<>();

		senderProps.put("exchange", exchange);
		senderProps.put("routingKey", routingKey);
		senderProps.put("queue", queue);
		senderProps.put("exchangeType", exchangeType.name());

		ServiceRegistration<BundleSender> bundleSenderRegistration =
			bundle
				.getBundleContext()
				.registerService(
					BundleSender.class,
					new BundleSenderImpl(
						_sender, exchange, routingKey), senderProps);

		ServiceRegistration<BundleReceiver> bundleReceiverRegistration =
			bundle
				.getBundleContext()
				.registerService(
					BundleReceiver.class,
					new BundleReceiverImpl(
						_receiverReactor, queue),
					new Hashtable<>(Collections.singletonMap("queue", queue)));

		_registrationMap.put(
			bundle, AutoCloseables.mergeAutoCloseableToSafe(
				bundleSenderRegistration::unregister,
				bundleReceiverRegistration::unregister,
				() -> _sender.unbind(binding).subscribe()));

		return null;
	}

	@Override
	public void modifiedService(
		ServiceReference<Binding> reference, Binding service) {

		removedService(reference, service);

		addingService(reference);

	}

	@Override
	public void removedService(
		ServiceReference<Binding> reference, Binding service) {

		AutoCloseables.AutoCloseableSafe autoCloseableSafe =
			_registrationMap.remove(reference.getBundle());

		if (autoCloseableSafe != null) {
			autoCloseableSafe.close();
		}

		_bundleContext.ungetService(reference);

	}

	private final Map<Bundle, AutoCloseables.AutoCloseableSafe> _registrationMap =
		new ConcurrentHashMap<>();

	private final Sender _sender;

	private final ReceiverReactor _receiverReactor;

	private final BundleContext _bundleContext;

	private static final Logger _log =
		LoggerFactory.getLogger(BindingServiceTrackerCustomizer.class);

}
