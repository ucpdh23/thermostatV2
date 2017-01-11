package es.xan.servant.parrot.translator;


import static es.xan.servant.parrot.translator.TranslationFacade.messageContains;
import static es.xan.servant.parrot.translator.TranslationFacade.messageIs;
import static es.xan.servant.parrot.translator.TranslationFacade.nextTokenTo;
import static es.xan.servant.parrot.translator.TranslationFacade.send;

import java.util.function.Function;
import java.util.function.Predicate;

import es.xan.servant.parrot.Constant;

public enum TranslatorEnum {
	RESPONSE_YES(Constant.QUESTIONS_VERTICLE_REPLY, false, messageIs("yes||si"), send("yes")), 
	RESPONSE_NO(Constant.QUESTIONS_VERTICLE_REPLY, false, messageIs("no"), send("no")), 
	BOILER(Constant.THERMOSTAT_VERTICLE, true, messageContains("boiler||caldera"), TranslationType.ON_OFF),
	TEMPERATURE(Constant.TEMPERATURE_VERTICLE, true, messageContains("temperature||temperatura"), TranslationType.GET),
	REMINDER(Constant.PARRONT_VERTICLE, false, messageContains("reminder"), TranslationType.COPY),
	BOILER_STATUS(Constant.OPERATION_BOILER_STATE_CHECKER, true, messageContains("checkBoilerStatus"), TranslationType.OPERATION),
	HOME(Constant.HOME_VERTICLE, true, messageContains("home||casa"), TranslationType.GET),
	RESET_SENSOR(Constant.SENSOR_VERTICLE, true, messageContains("sensor"), nextTokenTo("sensor")),
	;
	
	final Predicate<String> mPredicate;
	final TranslationType mType;
	final Function<String[],String> mFunction;
	final String mAddress;
	final boolean mForwarding;
	
	private TranslatorEnum(String address, boolean forwarding, Predicate<String> predicate, TranslationType type) {
		this.mAddress = address;
		this.mForwarding = forwarding;
		this.mPredicate = predicate;
		this.mType = type;
		this.mFunction = null;
	}
	
	private TranslatorEnum(String address, boolean forwarding, Predicate<String> predicate, Function<String[],String> variantFunction) {
		this.mAddress = address;
		this.mForwarding = forwarding;
		this.mPredicate = predicate;
		this.mType = TranslationType.DYNAMIC;
		this.mFunction = variantFunction;
	}

}
