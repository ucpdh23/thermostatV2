package es.xan.servant.parrot.translator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TranslationFacade {
	public static Translation translate(String text) {
		final Translation item = new Translation();
		
		fillTimeInformation(text, item);
		fillMessageAndAddress(text, item);
		
		return item;
	}

	private static void fillMessageAndAddress(String text, Translation item) {
		for (TranslatorEnum option : TranslatorEnum.values()) {
			for (String value : option.mSynonyms) {
				if (text.toLowerCase().contains(value)) {
					item.address = option.mAddress;
					item.forwarding = option.mForwarding;
					
					switch (option.mType) {
					case ON_OFF:
						if (text.toLowerCase().contains(" on") || text.toLowerCase().contains("encender")) {
							item.message = "on";
						} else if (text.toLowerCase().contains(" off") || text.toLowerCase().contains("apagar")) {
							item.message = "off";
						}
						break;
					case GET:
						item.message = "FW_GET";
						break;
					case COPY:
						item.message = text;
						break;
					case OPERATION:
						item.message = "OPERATION";
						break;
					}
				}
			}
		}
	}

	private static void fillTimeInformation(String message, Translation item) {
		if (message.toLowerCase().contains(" at ")) {
			addtimingInfo(message, item);
			if (message.toLowerCase().contains("every day")) {
				addEveryDayInfo(item);
			}
		} else if (message.toLowerCase().contains(" in ")) {
			addtimingInfoIn(message, item);
		}
	}
	
	protected static void addtimingInfoIn(String message, Translation item) {
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
	
	protected static void addtimingInfo(String message, Translation item) {
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
	
	private static void addEveryDayInfo(Translation item) {
		item.everyDay = true;
	}
}
