package es.xan.servant.parrot;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class ParrotVerticle extends Verticle implements CommunicationListener, Handler<Message<String>> {
	private GTalkManager channel;
	private EventBus eb;
	private Logger logger;
	
	private static final int WAITING_TIME = 30 * 1000;
	
	public void start() {
		logger = container.logger();

		channel = new GTalkManager(container.config().getObject("connection"));
		channel.setCommunicationListener(this);
		
		eb = vertx.eventBus();
		eb.registerHandler(Constant.PARRONT_VERTICLE, this);
		
		logger.debug("starting Parrot");
		
		vertx.setTimer(WAITING_TIME, t -> {
			eb.send(Constant.PARRONT_VERTICLE, "");
		});
	}
	
	String lastSender = "";
	
	@Override
	public void onMessage(String sender, String message) {
		lastSender = sender; 
		eb.send(Constant.BRAIN_VERTICLE, message, REPLY(sender));
	}

	@Override
	public void handle(Message<String> event) {
		if (!channel.isInit()) {
			channel.start();
			eb.publish(Constant.CORE_VERTICLE, Constant.CORE_CHAT_ACTIVE);
			return;
		}
		
		String sender = lastSender;
		String message = event.body();
				
		if (ParrotUtils.isAMessage(event.body())) {
			sender = ParrotUtils.getReceiver(event.body());
			message = ParrotUtils.getMessage(event.body());
		}
		
		if (Constant.PARROT_CREATE_CHAT.equals(message)) {
			try {
				channel.createChat(sender);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			channel.send(sender, message);
		}
		
	}
	
	private Handler<Message<String>> REPLY(final String receiver) {
		return (Message<String> event) -> {
			String message = event.body();
			
			if (message.equals(Constant.OK_MESSAGE)) {
				channel.send(receiver, "Your wish is my command");
			} else if (message.equals(Constant.KO_MESSAGE)) {
				channel.send(receiver, "I'm so sorry, but I do not undestand what you say");
			} else {
				channel.send(receiver, message);
			}
		};
	}
}
