package es.xan.servant.parrot;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class BrainVerticle extends Verticle implements Handler<Message<String>> {
	private EventBus eb;
	private Logger logger;
	
	final ScheduledExecutorService remember = Executors.newScheduledThreadPool(5);   
	
	public void start() {
		logger = container.logger();
		
		eb = vertx.eventBus();
		eb.registerHandler(Constant.COMMUNICATION_RECEIVE, this);
		
		logger.debug("started brain");
	}

	@Override
	public void handle(final Message<String> event) {
		final String message = event.body();
		logger.trace("message:" + message);
		
		Item item = new Item();
		
		if (message.toLowerCase().contains(" at ")) {
			addtimingInfo(message, item);
		} else if (message.toLowerCase().contains(" in ")) {
			addtimingInfoIn(message, item);
		}

		if (message.toLowerCase().contains("boiler") || message.toLowerCase().contains("caldera")) {
			item.address = Constant.THERMOSTAT;
			item.forwarding = true;
			
			if (message.toLowerCase().contains(" on") || message.toLowerCase().contains("encender")) {
				item.message = "on";
			} else if (message.toLowerCase().contains(" off") || message.toLowerCase().contains("apagar")) {
				item.message = "off";
			}
		} else if (message.toLowerCase().contains("reminder")) {
			item.address = Constant.COMMUNICATION_SENDER;
			item.forwarding = false;
			item.message = message;
		} else if (message.toLowerCase().contains("checkBoilerStatus")) {
			item.address = Constant.OPERATION_BOILER_STATE_CHECKER;
			item.forwarding = false;
			item.message = "OPERATION";
		} else if (message.toLowerCase().contains("temperature") || message.toLowerCase().contains("temperatura")) {
			item.address = Constant.TEMPERATURE;
			item.forwarding = true;
			item.message = "FW_GET";
		}
		
		send(item, event);
	}

	private void send(final Item item, Message<String> event) {
		if (ItemHelper.isScheduled(item)) {
			remember.schedule(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					eb.send(ItemHelper.createAdress(item), ItemHelper.createMessage(item), PARROT_HANDLER("Operation performed"));
					return null;
				}
				
			}, ItemHelper.getSchedule(item), TimeUnit.SECONDS);
		} else {
			if (ItemHelper.isForwarding(item)) {
				eb.send(ItemHelper.createAdress(item), ItemHelper.createMessage(item), FORWARDING_HANDLER(event));
			} else {
				eb.send(ItemHelper.createAdress(item), ItemHelper.createMessage(item));
			}
		}
		
	}


	protected static void addtimingInfoIn(String message, Item item) {
		final int indexOf = message.indexOf(" in ");
		
		final Pattern compile = Pattern.compile("(\\d+):(\\d+)");
		final Matcher matcher = compile.matcher(message);
		
		if (matcher.find(indexOf)) {
			final String str_min = matcher.group(1);
			final String str_sec = matcher.group(2);
			
			final int min = Integer.parseInt(str_min);
			final int sec = Integer.parseInt(str_sec);
				        
	        item.delayInfo = min * 60 + sec;
		}
	}
	
	protected static void addtimingInfo(String message, Item item) {
		final int indexOf = message.indexOf(" at ");
		
		final Pattern compile = Pattern.compile("(\\d+):(\\d+)");
		final Matcher matcher = compile.matcher(message);
		
		if (matcher.find(indexOf)) {
			final String str_hour = matcher.group(1);
			final String str_min = matcher.group(2);
			
			final int hour = Integer.parseInt(str_hour);
			final int min = Integer.parseInt(str_min);
			
			final LocalDateTime localNow = LocalDateTime.now();
			final ZoneId currentZone = ZoneId.of("Europe/Madrid");
			final ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
			ZonedDateTime zonedNextScheduled = zonedNow.withHour(hour).withMinute(min).withSecond(0);
	        if(zonedNow.compareTo(zonedNextScheduled) > 0)
	        	zonedNextScheduled = zonedNextScheduled.plusDays(1);

	        final Duration duration = Duration.between(zonedNow, zonedNextScheduled);
	        final long initialDelay = duration.getSeconds();
	        
	        item.delayInfo = initialDelay;
		}
	}

	public Handler<Message<String>> PARROT_HANDLER(final String message) { return new Handler<Message<String>>() {

		@Override
		public void handle(Message<String> event) {
			eb.send(Constant.COMMUNICATION_RECEIVE, message);
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


class ItemHelper {
	static String createAdress(Item item) {
		return item.address;
	}
	
	public static boolean isScheduled(Item item) {
		return item.delayInfo > 0;
	}

	static String createMessage(Item item) {
		return item.message;
	}
	
	static boolean isForwarding(Item item) {
		return item.forwarding;
	}
	
	static long getSchedule(Item item) {
		return item.delayInfo;
	}
}

class Item {
	long delayInfo;
	String address;
	String message;
	boolean forwarding;
}
