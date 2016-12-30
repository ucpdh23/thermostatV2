package es.xan.servant.parrot;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class WebServerVerticle extends Verticle {

	private EventBus eb;

	private JsonObject configuration;
	
	private static final String CONFIG = "webService";

	public void start() {
		eb = vertx.eventBus();
		configuration = container.config().getObject(CONFIG);
		final int port = configuration.getInteger("port");
		
		final RouteMatcher matcher = buildMatcher();
		
		vertx.createHttpServer()
			.requestHandler(matcher)
			.listen(port);

		container.logger().info("Webserver started, listening on port: " + port);
	}

	private RouteMatcher buildMatcher() {
		final RouteMatcher matcher = new RouteMatcher();
		
		matcher.get("/temperature/:place/:value", request -> {
				String value = request.params().get("value");
				String place = request.params().get("place");
				eb.send(Constant.TEMPERATURE_VERTICLE, value + "#" + place);
				
				request.response().end("ok");
			});
		
		matcher.get("/listTemperature", request -> {
				eb.send(Constant.TEMPERATURE_VERTICLE, "LIST", (Message<String> event)
						-> request.response().end(event.body()));
			});
		
		matcher.get("/boiler/:operation", request -> {
				eb.send(Constant.THERMOSTAT_VERTICLE, request.params().get("operation"));

				request.response().end("ok");
			});
		
		return matcher;
	}
}
