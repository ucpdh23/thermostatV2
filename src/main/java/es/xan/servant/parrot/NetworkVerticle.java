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
	List<Device> quarantine;
	
	public void start() {
		System.out.println("starting Network...");
		devices = new ArrayList<RouterPageManager.Device>();
		quarantine = new ArrayList<RouterPageManager.Device>();
		
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
				List<Device> newsInQuarantine = resolveIntersectionDevices(newItems, quarantine);
				List<Device> newsToNotify = resolveDiffsDevices(quarantine, newItems);
				
				List<Device> removedToNotify = resolveDiffsDevices(newsInQuarantine, quarantine);
				
				List<Device> lost = resolveDiffsDevices(newDevices, devices);
				
				if (!newsToNotify.isEmpty()) {
					vertx.eventBus().publish(Constant.NEW_NETWORK_DEVICES_MESSAGE, NetworkUtils.createArray(newsToNotify));
				}
				
				if (!removedToNotify.isEmpty()) {
					vertx.eventBus().publish(Constant.REM_NETWORK_DEVICES_MESSAGE, NetworkUtils.createArray(removedToNotify));
				}
				
				devices = newDevices;
				quarantine = lost;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private List<Device> resolveIntersectionDevices(List<Device> newDevices, List<Device> quarantine2) {
		return newDevices.stream().filter(it -> exists(it, quarantine2)).collect(Collectors.toList());
	}


	private List<Device> resolveDiffsDevices(List<Device> curDevices, List<Device> newDevices) {
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
