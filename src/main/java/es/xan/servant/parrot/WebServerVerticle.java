package es.xan.servant.parrot;

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class WebServerVerticle extends Verticle {

	private EventBus eb;

	private JsonObject configuration;
	
	private static final String CONFIG = "webService";

	public void start() {
		eb = vertx.eventBus();
		configuration = container.config().getObject(CONFIG);

		
		RouteMatcher matcher = new RouteMatcher();
		matcher.get("/temperature/:place/:value", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				String value = request.params().get("value");
				String place = request.params().get("place");
				eb.send(Constant.TEMPERATURE, value + "#" + place);
				
				System.out.println("value:" + value);
				
				request.response().end("ok");
			}
		});
		
		matcher.get("/listTemperature", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				Handler<Message<String>> replyHandler = new Handler<Message<String>>() {

					@Override
					public void handle(Message<String> event) {
						request.response().end(event.body());
					}
					
				};
				eb.send(Constant.TEMPERATURE, "LIST", replyHandler);
			}
		});
		
		matcher.get("/boiler/:operation", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				eb.send(Constant.THERMOSTAT, request.params().get("operation"));
				
				request.response().end("ok");
			}
		});
		
		final int port = configuration.getInteger("port");
		vertx.createHttpServer().requestHandler(matcher).listen(port);

		container.logger().info("Webserver started, listening on port: " + port);
	}
}
