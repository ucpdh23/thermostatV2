package es.xan.servant.parrot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

import es.xan.servant.network.RouterPageManager;
import es.xan.servant.network.RouterPageManager.Device;

public class NetworkVerticle extends Verticle implements Handler<Message<String>>  {
	
	RouterPageManager manager = null;
	
	List<Device> devices;
	
	public void start() {
		System.out.println("starting Network...");
		devices = new ArrayList<RouterPageManager.Device>();
		manager = new RouterPageManager(container.config().getObject("router"));
	
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
		return newDevices.stream().filter(it -> !exists(it, curDevices)).collect(Collectors.toList());
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
