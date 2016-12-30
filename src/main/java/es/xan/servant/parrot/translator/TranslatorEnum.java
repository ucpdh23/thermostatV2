package es.xan.servant.parrot.translator;

import es.xan.servant.parrot.Constant;

public enum TranslatorEnum {
	BOILER(Constant.THERMOSTAT_VERTICLE, true, new String[]{"boiler","caldera"}, TranslationType.ON_OFF),
	TEMPERATURE(Constant.TEMPERATURE_VERTICLE, true, new String[]{"temperature","temperatura"}, TranslationType.GET),
	REMINDER(Constant.PARRONT_VERTICLE, false, new String[]{"reminder"}, TranslationType.COPY),
	BOILER_STATUS(Constant.OPERATION_BOILER_STATE_CHECKER, true, new String[] {"checkBoilerStatus"}, TranslationType.OPERATION),
	HOME(Constant.HOME_VERTICLE, true, new String[] {"home"}, TranslationType.GET),
	;
	
	final String[] mSynonyms;
	final TranslationType mType;
	final String mAddress;
	final boolean mForwarding;
	
	private TranslatorEnum(String address, boolean forwarding, String[] synonyms, TranslationType type) {
		this.mAddress = address;
		this.mForwarding = forwarding;
		this.mSynonyms = synonyms;
		this.mType = type;
	}
}
