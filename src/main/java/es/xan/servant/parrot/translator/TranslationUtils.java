package es.xan.servant.parrot.translator;

public class TranslationUtils {
	public static String createAdress(Translation item) {
		return item.address;
	}
	
	public static boolean isEveryDay(Translation item) {
		return item.everyDay;
	}

	public static boolean isScheduled(Translation item) {
		return item.delayInfo > 0;
	}

	public static String createMessage(Translation item) {
		return item.message;
	}
	
	public static boolean isForwarding(Translation item) {
		return item.forwarding;
	}
	
	public static long getSchedule(Translation item) {
		return item.delayInfo;
	}
}
