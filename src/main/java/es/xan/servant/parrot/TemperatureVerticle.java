package es.xan.servant.parrot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class TemperatureVerticle extends Verticle implements
		Handler<Message<String>> {

	private static final int BUFFER_SIZE = 5000;
	
	private Logger logger;

	private EventBus eb;
	
	private static class Measure {
		Float temperature;
		Calendar timestamp;
		
		public Measure(Float temperature) {
			this.temperature = temperature;
			this.timestamp = Calendar.getInstance();
		}
	}

	static class RoomTemperatureData {
		Measure[] measures;
		int nextIndex = 0;
		
		public RoomTemperatureData() {
			this.measures = new Measure[BUFFER_SIZE];
		}
		
		public void register(Measure measure) {
			this.measures[this.nextIndex] = measure;
			this.nextIndex = (this.nextIndex + 1) % BUFFER_SIZE;

		}
	}
	
	private Map<String, RoomTemperatureData> storage = new HashMap<>();

	private long noDataTimeoutTimer;
	

	@Override
	public void start() {
		logger = container.logger();

		eb = vertx.eventBus();
		eb.registerHandler(Constant.TEMPERATURE, this);

		logger.debug("started Temperature");
		
		noDataTimeoutTimer = vertx.setTimer(1000 * 60 * 60, NO_TEMPERATURE_TIMER_HANDLER);
	}
	
	final Handler<Long> NO_TEMPERATURE_TIMER_HANDLER = (Long event) -> {
		eb.publish(Constant.NO_TEMPERATURE_INFO, "");
	};

	private static Pattern TIME_VALUE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)#(.*)");
	
	@Override
	public void handle(Message<String> event) {
		vertx.cancelTimer(noDataTimeoutTimer);
		noDataTimeoutTimer = vertx.setTimer(1000 * 60 * 60, NO_TEMPERATURE_TIMER_HANDLER);
		
		final String value = event.body();
		
		Matcher matcher = TIME_VALUE_PATTERN.matcher(value);
		if (matcher.matches()) {
			final Measure temperatureInfo = new Measure(Float.valueOf(matcher.group(1)));
			storeInBuffer(matcher.group(3), temperatureInfo);
			
		} else if (value.equals("FW_GET")) {
			if (storage.isEmpty()) {
				event.reply(Constant.KO_MESSAGE);
			} else {
				StringBuilder builder = new StringBuilder();
				for (Entry<String, RoomTemperatureData> item : storage.entrySet()) {
					String place = item.getKey();
					RoomTemperatureData temperatureData = item.getValue();
					
					int prevIndex = ((temperatureData.nextIndex - 1) + 5000) % 5000;
					Measure temp = temperatureData.measures[prevIndex];
					
					if (temp != null) {
						builder.append("Place:").append(place).append(":").append(temp.temperature.toString() + " at " + temp.timestamp.getTime()).append("\n");
					}
				}
				event.reply(builder.toString());
			}
		} else if("LIST".equals(value)) {
			StringBuilder builder = new StringBuilder();
			
			for (Entry<String, RoomTemperatureData> item : storage.entrySet()) {
				String place = item.getKey();
				RoomTemperatureData temperatureData = item.getValue();
				
				builder.append("Place:").append(place).append("\n");
				for (int i=0; i < temperatureData.nextIndex; i++) {
					Measure temp = temperatureData.measures[i];
					builder.append("" + temp.timestamp.getTime() + ": " + temp.temperature.toString() + "\n");
				}

			}
			event.reply(builder.toString());
		} else {
			logger.warn("lossing " + value);
		}

	}

	private void storeInBuffer(String group, Measure temperatureInfo) {
		final RoomTemperatureData temperatureInfoData = storage.computeIfAbsent(group, (x) -> { return new RoomTemperatureData(); });
		temperatureInfoData.register(temperatureInfo);
	}

}
