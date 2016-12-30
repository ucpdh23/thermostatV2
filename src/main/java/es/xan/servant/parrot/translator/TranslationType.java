package es.xan.servant.parrot.translator;

import org.vertx.java.core.eventbus.Message;

public enum TranslationType {
	ON_OFF(""),
	GET("FW_GET"),
	COPY(""),
	OPERATION("");
	
	String matching;
	
	private TranslationType(String matcher) {
		this.matching = matcher;
	}
	
	public boolean matchEvent(Message<?> event) {
		return matching.equals(event.body());
	}
}
