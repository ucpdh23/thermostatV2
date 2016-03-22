package es.xan.servant.parrot;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class ParrotVerticle extends Verticle implements CommunicationListener, Handler<Message<String>> {
	private GTalkManager mouth;
	private EventBus eb;
	private Logger logger;
	
	public void start() {
		logger = container.logger();

		mouth = new GTalkManager(container.config().getObject("connection"));
		mouth.setCommunicationListener(this);
		
		eb = vertx.eventBus();
		eb.registerHandler(Constant.COMMUNICATION_SENDER, this);
		
		logger.debug("starting Parrot");
		
		vertx.setTimer(30 * 1000, new Handler<Long>() {
			   public void handle(Long t) {
			         eb.send(Constant.COMMUNICATION_SENDER, "");
			   }
		});
	}
	
	String lastSender = "";
	
	@Override
	public void onMessage(String sender, String message) {
		lastSender = sender; 
		eb.send(Constant.COMMUNICATION_RECEIVE, message, REPLY(sender));
	}

	@Override
	public void handle(Message<String> event) {
		if (!mouth.isInit()) {
			mouth.start();
			eb.publish(Constant.CORE, Constant.CORE_CHAT_ACTIVE);
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
				mouth.createChat(sender);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			mouth.send(sender, message);
		}
		
	}
	
	private Handler<Message<String>> REPLY(final String receiver) {
		return new Handler<Message<String>>() {
		@Override
		public void handle(Message<String> event) {
			String message = event.body();
			
			if (message.equals(Constant.OK_MESSAGE)) {
				mouth.send(receiver, "Your wish is my command");
			} else if (message.equals(Constant.KO_MESSAGE)) {
				mouth.send(receiver, "I'm so sorry, but I do not undestand what you say");
			} else {
				mouth.send(receiver, message);
			}
			
		}
	};
	}
}
