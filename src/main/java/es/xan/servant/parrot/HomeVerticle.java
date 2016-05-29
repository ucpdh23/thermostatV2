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

import es.xan.servant.network.RouterManager.Device;

public class HomeVerticle extends Verticle implements Handler<Message<JsonElement>> {
	
	Map<String, Person> people = new HashMap<>();
	private String mBoss;
	
	public void start() {
		System.out.println("starting Home...");
		vertx.eventBus().registerHandler(Constant.NEW_NETWORK_DEVICES_MESSAGE, this);
		vertx.eventBus().registerHandler(Constant.REM_NETWORK_DEVICES_MESSAGE, this);
		
		JsonArray configuration = container.config().getArray("home");
		
		System.out.println("after config " + configuration);
		
		for (int i=0; i < configuration.asArray().size(); i++) {
			JsonObject person = (JsonObject) configuration.asArray().get(i);
			String name = person.getString("name");
			String mac = person.getString("mac");
			
			
			Person person_ = new Person();
			person_.name = name;
			person_.inHome = false;
			
			System.out.println("peson:" + person_.name);
			
			people.put(mac, person_);
		}
		
		JsonArray masters = container.config().getArray("masters");
		
		LinkedHashMap<String,String> emailInfo = (LinkedHashMap<String, String>) (masters.toArray()[0]);
		this.mBoss = emailInfo.get("email");
		
		System.out.println("started Home");
	}

	@Override
	public void handle(Message<JsonElement> event) {
		String address = event.address();
		System.out.println("home " + address);
		if (address.equals(Constant.NEW_NETWORK_DEVICES_MESSAGE)) {
			JsonArray asArray = event.body().asArray();
			
			for (int i=0;i < asArray.size(); i++) {
				Device device = NetworkUtils.asDevice(asArray.get(i));
				
				Person person = people.get(device.mac);
				
				if (person != null) {
					if (!person.inHome) {
						person.inHome = true;
						vertx.eventBus().publish(Constant.PERSON_AT_HOME, person.name);
						vertx.eventBus().publish(Constant.COMMUNICATION_SENDER, ParrotUtils.createMessage(this.mBoss, person.name + " at home"));
					}
				} else 
					System.out.println("not found");
			}
			
		} else if (address.equals(Constant.REM_NETWORK_DEVICES_MESSAGE)) {
			JsonArray asArray = event.body().asArray();
			
			for (int i=0;i < asArray.size(); i++) {
				Device device = NetworkUtils.asDevice(asArray.get(i));
				
				Person person = people.get(device.mac);
				
				if (person != null) {
					
					if (person.inHome) {
						person.inHome = false;
						vertx.eventBus().publish(Constant.PERSON_LEAVE_HOME, person.name);
						vertx.eventBus().publish(Constant.COMMUNICATION_SENDER, ParrotUtils.createMessage(this.mBoss, person.name + " leave home"));
					}
				} else 
					System.out.println("not found");
			}
		}
		
	}
	
	static class Person {
		String name;
		boolean inHome;
	}
}
