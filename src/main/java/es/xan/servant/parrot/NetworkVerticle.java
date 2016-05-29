package es.xan.servant.parrot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

import es.xan.servant.network.RouterManager;
import es.xan.servant.network.RouterManager.Device;

public class NetworkVerticle extends Verticle implements Handler<Message<String>>  {
	
	RouterManager manager = null;
	
	List<Device> devices;
	
	public void start() {
		System.out.println("starting Network...");
		devices = new ArrayList<RouterManager.Device>();
		manager = new RouterManager(container.config().getObject("router"));
	
		vertx.eventBus().registerHandler(Constant.CHECK_NETWORK_MESSAGE, this);
		
		vertx.setPeriodic(300000, id -> {
			vertx.eventBus().send(Constant.CHECK_NETWORK_MESSAGE, "true");
		});
		
		System.out.println("started Network...");
	}

	@Override
	public void handle(Message<String> event) {
		if (Constant.CHECK_NETWORK_MESSAGE.equals(event.address())) {
			try {
				List<Device> newDevices = manager.getDevices();
				
				List<Device> newItems = resolveDiffsDevices(devices, newDevices);
				List<Device> remItems = resolveDiffsDevices(newDevices, devices);
				
				if (!newItems.isEmpty()) {
					vertx.eventBus().publish(Constant.NEW_NETWORK_DEVICES_MESSAGE, NetworkUtils.createArray(newItems));
				}
				
				if (!remItems.isEmpty()) {
					vertx.eventBus().publish(Constant.REM_NETWORK_DEVICES_MESSAGE, NetworkUtils.createArray(remItems));
				}
				
				devices = newDevices;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private List<Device> resolveDiffsDevices(List<Device> curDevices,
			List<Device> newDevices) {
		List<Device> result = new ArrayList<>();
		for (Device newItem : newDevices) {
			if (!exists(newItem, curDevices)) {
				result.add(newItem);
			}
		}

		return result;
	}

	private boolean exists(Device newItem, List<Device> newDevices) {
		for (Device item : newDevices) {
			if (newItem.mac.equals(item.mac)) {
				return true;
			}
		}
		
		return false;
	}
	
	
}
