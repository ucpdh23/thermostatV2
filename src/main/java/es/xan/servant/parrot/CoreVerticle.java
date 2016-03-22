package es.xan.servant.parrot;

import java.util.LinkedHashMap;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class CoreVerticle extends Verticle implements Handler<Message<String>> {
	private EventBus eb;
	private Logger logger;
	private JsonArray configuration;
		
	public void start() {
		logger = container.logger();
		configuration = container.config().getArray("masters");
		
		eb = vertx.eventBus();
		eb.registerHandler(Constant.CORE, this);
		
		logger.debug("started core");
	}
	@Override
	public void handle(Message<String> event) {
		final String body = event.body();
		
		if (Constant.CORE_CHAT_ACTIVE.equals(body)) {
			for (Object item : configuration.toArray()) {
				LinkedHashMap<String,String> emailInfo = (LinkedHashMap<String, String>) item;
				String email = emailInfo.get("email");
				
				eb.publish(Constant.COMMUNICATION_SENDER, ParrotUtils.createMessage(email, Constant.PARROT_CREATE_CHAT));
			}
		}
	}

}
