package es.xan.servant.parrot;

public class ParrotUtils {
	public static String getReceiver(String fullMessage) {
		return fullMessage.split("###")[0];
	}
	
	public static String getMessage(String fullMessage) {
		return fullMessage.split("###")[1];
	}
	
	public static String createMessage(String receiver, String message) {
		return receiver + "###" + message;
	}
	
	public static boolean isAMessage(String text) {
		return text.contains("###");
	}
}
