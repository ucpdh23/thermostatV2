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

	private Logger logger;

	private EventBus eb;
	
	static class TemperatureInfoData {
		TemperatureInfo[] buffer;
		int nextIndex = 0;
		
		public TemperatureInfoData() {
			this.buffer = new TemperatureInfo[5000];
		}
	}
	
	Map<String, TemperatureInfoData> data = new HashMap<>();
	
	private static class TemperatureInfo {
		Float value;
		Calendar date;
	}

	@Override
	public void start() {
		logger = container.logger();

		eb = vertx.eventBus();
		eb.registerHandler(Constant.TEMPERATURE, this);

		logger.debug("started Temperature");
	}

	Pattern VALUE = Pattern.compile("(\\d+(\\.\\d+)?)#(.*)");
	
	@Override
	public void handle(Message<String> event) {
		final String value = event.body();
		
		Matcher matcher = VALUE.matcher(value);
		if (matcher.matches()) {
			TemperatureInfo temperatureInfo = new TemperatureInfo();
			temperatureInfo.date = Calendar.getInstance();
			temperatureInfo.value = Float.valueOf(matcher.group(1));
			
			storeInBuffer(matcher.group(3), temperatureInfo);
		} else if (value.equals("FW_GET")) {
			if (data.isEmpty()) {
				event.reply(Constant.KO_MESSAGE);
			} else {
				StringBuilder builder = new StringBuilder();
				for (Entry<String, TemperatureInfoData> item : data.entrySet()) {
					String place = item.getKey();
					TemperatureInfoData temperatureData = item.getValue();
					
					int prevIndex = ((temperatureData.nextIndex - 1) + 5000) % 5000;
					TemperatureInfo temp = temperatureData.buffer[prevIndex];
					
					if (temp == null) {
					} else {
						builder.append("Place:").append(place).append(":").append(temp.value.toString() + " at " + temp.date.getTime()).append("\n");
					}
				}
				event.reply(builder.toString());
			}
		} else if("LIST".equals(value)) {
			StringBuilder builder = new StringBuilder();
			
			for (Entry<String, TemperatureInfoData> item : data.entrySet()) {
				String place = item.getKey();
				TemperatureInfoData temperatureData = item.getValue();
				
				builder.append("Place:").append(place).append("\n");
				for (int i=0; i < temperatureData.nextIndex; i++) {
					TemperatureInfo temp = temperatureData.buffer[i];
					builder.append("" + temp.date.getTime() + ": " + temp.value.toString() + "\n");
				}

			}
			event.reply(builder.toString());
		} else {
			logger.warn("lossing " + value);
		}

	}

	private void storeInBuffer(String group, TemperatureInfo temperatureInfo) {
		TemperatureInfoData temperatureInfoData = data.get(group);
		
		if (temperatureInfoData == null) {
			temperatureInfoData = new TemperatureInfoData();
		}
		
		data.put(group, temperatureInfoData);
		
		
		
		temperatureInfoData.buffer[temperatureInfoData.nextIndex] = temperatureInfo;
		temperatureInfoData.nextIndex = (temperatureInfoData.nextIndex + 1) % 5000;
	}

}
