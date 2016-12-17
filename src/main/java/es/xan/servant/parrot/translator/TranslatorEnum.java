package es.xan.servant.parrot.translator;

import es.xan.servant.parrot.Constant;

public enum TranslatorEnum {
	BOILER(Constant.THERMOSTAT, true, new String[]{"boiler","caldera"}, TranslationType.ON_OFF),
	TEMPERATURE(Constant.TEMPERATURE, true, new String[]{"temperature","temperatura"}, TranslationType.GET),
	REMINDER(Constant.COMMUNICATION_SENDER, false, new String[]{"reminder"}, TranslationType.COPY),
	BOILER_STATUS(Constant.OPERATION_BOILER_STATE_CHECKER, true, new String[] {"checkBoilerStatus"}, TranslationType.OPERATION)
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
