package es.xan.servant.parrot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import es.xan.servant.network.RouterManager.Device;

public class NetworkUtils {
	public static JsonArray createArray(List<Device> items) {
		JsonArray array = new JsonArray(transformList(items));
		
		return array;
	}

	private static List<Map<String,Object>> transformList(List<Device> items) {
		List<Map<String,Object>> result = new ArrayList<>();
		for (Device item : items) {
			result.add(transformItem(item));
		}
		return result;
	}

	private static Map<String, Object> transformItem(Device item) {
		Map<String,Object> result = new HashMap<>();
		result.put("mac", item.mac);
		result.put("name", item.name);
		result.put("ip", item.ip);
		result.put("active", item.active);
		
		return result;
	}

	public static Device asDevice(JsonObject object) {
		Device result = new Device();
		result.active = object.getBoolean("active");
		result.ip = object.getString("ip");
		result.name = object.getString("name");
		result.mac = object.getString("mac");
		
		return result;
	}
}
