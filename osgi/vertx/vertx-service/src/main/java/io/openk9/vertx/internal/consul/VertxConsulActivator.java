package io.openk9.vertx.internal.consul;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import java.util.Map;
import java.util.Objects;

@Component(
	immediate = true,
	service = VertxConsulActivator.class,
	property = {
		"host=consul",
		"port:Integer=8500",
		"scan-period:Integer=2000"
	}
)
@Services(provides = {
	@ProvideService(ConsulClient.class)
})
public class VertxConsulActivator extends BaseActivator {

	@Activate
	void activate(Map<String, Object> config, BundleContext context) {

		prevConfig = config;

		ConsulClientOptions opts =
			new ConsulClientOptions(JsonObject.mapFrom(config));

		_consulClient = ConsulClient.create(_vertx, opts);

		_serviceRegistration =
			context.registerService(
				ConsulClient.class, _consulClient, null);

	}

	@Modified
	void modified(Map<String, Object> config, BundleContext bundleContext) {
		if (!Objects.equals(prevConfig, config)) {
			deactivate();
			activate(config, bundleContext);
		}
	}

	@Deactivate
	void deactivate() {
		try {
			_consulClient.close();
		}
		catch (Exception e) {
			// ignore
		}
		_consulClient = null;
		_serviceRegistration.unregister();
		_serviceRegistration = null;
	}

	private ConsulClient _consulClient;

	private ServiceRegistration _serviceRegistration;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private Vertx _vertx;

	private transient Map<String, Object> prevConfig = null;

}
