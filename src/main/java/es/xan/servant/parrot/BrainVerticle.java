package es.xan.servant.parrot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import es.xan.servant.parrot.translator.Translation;
import es.xan.servant.parrot.translator.TranslationFacade;
import es.xan.servant.parrot.translator.TranslationUtils;

public class BrainVerticle extends Verticle implements Handler<Message<String>> {
	private EventBus eb;
	private Logger logger;
	
	final ScheduledExecutorService remember = Executors.newScheduledThreadPool(5);   
	
	public void start() {
		logger = container.logger();
		
		eb = vertx.eventBus();
		eb.registerHandler(Constant.BRAIN_VERTICLE, this);
		
		logger.debug("started brain");
	}

	@Override
	public void handle(final Message<String> event) {
		final String message = event.body();
		logger.trace("message:" + message);
		
		final Translation item = TranslationFacade.translate(message);
		
		send(item, event);
	}

	private void send(final Translation item, Message<String> event) {
		if (TranslationUtils.isScheduled(item)) {
			if (TranslationUtils.isEveryDay(item)) {
				remember.scheduleAtFixedRate(() -> {
						eb.send(TranslationUtils.createAdress(item), TranslationUtils.createMessage(item), PARROT_HANDLER("Operation performed"));
					}, TranslationUtils.getSchedule(item), 24 * 60 * 60, TimeUnit.SECONDS);
				
			} else {
				remember.schedule(() -> {
						eb.send(TranslationUtils.createAdress(item), TranslationUtils.createMessage(item), PARROT_HANDLER("Operation performed"));
						return null;
					}, TranslationUtils.getSchedule(item), TimeUnit.SECONDS);
			}
		} else {
			if (TranslationUtils.isForwarding(item)) {
				eb.send(TranslationUtils.createAdress(item), TranslationUtils.createMessage(item), FORWARDING_HANDLER(event));
			} else {
				eb.send(TranslationUtils.createAdress(item), TranslationUtils.createMessage(item));
			}
		}
	}

	public Handler<Message<String>> PARROT_HANDLER(final String message) { return new Handler<Message<String>>() {

		@Override
		public void handle(Message<String> event) {
			eb.send(Constant.BRAIN_VERTICLE, message);
		}
	};
	}
	public Handler<Message<String>> FORWARDING_HANDLER(final Message<String> trigger) { return new Handler<Message<String>>() {

		@Override
		public void handle(Message<String> event) {
			trigger.reply(event.body());
		}
	};
	}
}


