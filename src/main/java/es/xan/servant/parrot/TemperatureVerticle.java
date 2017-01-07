package es.xan.servant.parrot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import es.xan.servant.parrot.translator.TranslationType;

public class TemperatureVerticle extends Verticle implements Handler<Message<String>> {

	private static final int BUFFER_SIZE = 5000;
	
	private Logger logger;

	private static class Measure {
		Float temperature;
		Calendar timestamp;
		
		public Measure(Float temperature) {
			this.temperature = temperature;
			this.timestamp = Calendar.getInstance();
		}
	}

	static class RoomTemperatureData {
		private final Vertx mVertx;
		private final Measure[] measures;
		private final String mRoom;
		
		private int nextIndex = 0;
		private long noDataTimeoutTimer;
		
		public RoomTemperatureData(Vertx vertx, String room) {
			this.mVertx = vertx;
			this.mRoom = room;
			
			this.measures = new Measure[BUFFER_SIZE];
			this.noDataTimeoutTimer = this.mVertx.setTimer(1000 * 60 * 60, createTimerForRoom(this.mRoom));
		}
		
		public void register(Measure measure) {
			this.mVertx.cancelTimer(noDataTimeoutTimer);
			noDataTimeoutTimer = mVertx.setTimer(1000 * 60 * 60, createTimerForRoom(this.mRoom));
				
			this.measures[this.nextIndex] = measure;
			this.nextIndex = (this.nextIndex + 1) % BUFFER_SIZE;
		}
		
		private Handler<Long> createTimerForRoom(String room) {
			return (Long event) -> {
				this.mVertx.eventBus().publish(Constant.EVENT_NO_TEMPERATURE_INFO, room);
			};
		}
	}
	
	private Map<String, RoomTemperatureData> storage = new HashMap<>();

	@Override
	public void start() {
		logger = container.logger();

		vertx.eventBus().registerHandler(Constant.TEMPERATURE_VERTICLE, this);

		logger.debug("started Temperature");
	}
	
	private static Pattern TIME_VALUE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)#(.*)");
	
	@Override
	public void handle(Message<String> event) {
		final String value = event.body();
		
		Matcher matcher = TIME_VALUE_PATTERN.matcher(value);
		if (matcher.matches()) {
			final Measure temperatureInfo = new Measure(Float.valueOf(matcher.group(1)));
			storeInBuffer(matcher.group(3), temperatureInfo);
			
		} else if (TranslationType.GET.matchEvent(event)) {
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

	private void storeInBuffer(String room, Measure temperatureInfo) {
		final RoomTemperatureData temperatureInfoData = storage.computeIfAbsent(room, (x) -> { return new RoomTemperatureData(vertx, room); });
		temperatureInfoData.register(temperatureInfo);
	}

}
