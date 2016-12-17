package es.xan.servant.parrot;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import es.xan.servant.network.RouterPageManager.Device;

public class HomeVerticle extends Verticle implements Handler<Message<JsonElement>> {
	
	private Map<String, Person> population = new HashMap<>();
	private String mBoss;
	
	public void start() {
		System.out.println("starting Home...");
		
		vertx.eventBus().registerHandler(Constant.NEW_NETWORK_DEVICES_MESSAGE,	this);
		vertx.eventBus().registerHandler(Constant.REM_NETWORK_DEVICES_MESSAGE,	this);
		vertx.eventBus().registerHandler(Constant.NO_TEMPERATURE_INFO,			this);
		
		final JsonArray configuration = container.config().getArray("home");
		
		System.out.println("after config " + configuration);
		
		for (int i=0; i < configuration.asArray().size(); i++) {
			JsonObject personInfo = (JsonObject) configuration.asArray().get(i);
			String name = personInfo.getString("name");
			String mac  = personInfo.getString("mac");
			
			
			Person person_ = new Person();
			person_.name = name;
			person_.inHome = false;
			
			System.out.println("peson:" + person_.name);
			
			population.put(mac, person_);
		}
		
		JsonArray masters = container.config().getArray("masters");
		
		LinkedHashMap<String,String> emailInfo = (LinkedHashMap<String, String>) (masters.toArray()[0]);
		this.mBoss = emailInfo.get("email");
		
		System.out.println("started Home");
	}

	@Override
	public void handle(Message<JsonElement> event) {
		String address = event.address();
		
		switch (address) {
		case Constant.NEW_NETWORK_DEVICES_MESSAGE:
		case Constant.REM_NETWORK_DEVICES_MESSAGE:
			JsonArray asArray = event.body().asArray();
			
			for (int i=0;i < asArray.size(); i++) {
				Device device = NetworkUtils.asDevice(asArray.get(i));
				
				Person person = population.get(device.mac);
				
				if (person == null) continue;
				
				if (Constant.REM_NETWORK_DEVICES_MESSAGE.equals(address) && person.inHome) {
					updatePerson(person, false);
				} else if (Constant.NEW_NETWORK_DEVICES_MESSAGE.equals(address) && !person.inHome) {
					updatePerson(person, true);
				}
			}
			break;
			
		case Constant.NO_TEMPERATURE_INFO:
			vertx.eventBus().publish(Constant.COMMUNICATION_SENDER, ParrotUtils.createMessage(this.mBoss, "no temperature info since 1 hour"));
			break;
		}
		
	}
	
	private void updatePerson(Person person, boolean atHome) {
		person.inHome = atHome;
		if (atHome) {
			vertx.eventBus().publish(Constant.PERSON_AT_HOME, person.name);
			vertx.eventBus().publish(Constant.COMMUNICATION_SENDER, ParrotUtils.createMessage(this.mBoss, person.name + " at home"));
		} else {
			vertx.eventBus().publish(Constant.PERSON_LEAVE_HOME, person.name);
			vertx.eventBus().publish(Constant.COMMUNICATION_SENDER, ParrotUtils.createMessage(this.mBoss, person.name + " leave home"));
		}
	}

	static class Person {
		String name;
		boolean inHome;
	}
}
