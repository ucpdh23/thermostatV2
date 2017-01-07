package es.xan.servant.parrot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SensorVerticle extends Verticle implements Handler<Message<String>> {
	private EventBus eb;
	private Logger logger;
	
	private String host;
	private String login;
	private String password;
	
	private Map<String,String> sensors;
	
	public void start() {
		logger = container.logger();
		
		loadConfiguration(container.config().getObject("sensors"));
		
		eb = vertx.eventBus();
		eb.registerHandler(Constant.SENSOR_VERTICLE, this);
		
		logger.debug("started sensors");
	}
	
	private void loadConfiguration(JsonObject config) {
		host = config.getString("server");
		login = config.getString("usr");
		password = config.getString("pws");
		
		loadSensors(config.getArray("items"));
	}

	private void loadSensors(JsonArray array) {
		sensors = new HashMap<>();
		
		for (int i=0; i < array.asArray().size(); i++) {
			final JsonObject sensor = (JsonObject) array.asArray().get(i);
			String name = sensor.getString("name");
			String command = sensor.getString("command");
			
			sensors.put(name, command);
		}
	}

	@Override
	public void handle(Message<String> event) {
		final String body = event.body();

		logger.info("Asking to reset sensor " + body);
		final String command = sensors.get(body);
		
		if (command == null) {
			logger.warn("Not found sensor " + body);
			event.reply(Constant.KO_MESSAGE);
		} else {
			boolean result = false;
			try {
				result = runRemoteCommand(command);
			} catch (JSchException | IOException e) {
				e.printStackTrace();
			}
			
			event.reply(result? Constant.OK_MESSAGE : Constant.KO_MESSAGE);
		}
	}

	private boolean runRemoteCommand(String command) throws JSchException, IOException {
		JSch jsch = new JSch();
		
		Session session = jsch.getSession(login, host, 22);
		session.setConfig("StrictHostKeyChecking", "no");
		session.setPassword(password);
		session.connect();
			 
		//create the excution channel over the session
		ChannelExec channelExec = (ChannelExec)session.openChannel("exec");
			 
		// Gets an InputStream for this channel. All data arriving in as messages from the remote side can be read from this stream.
		InputStream in = channelExec.getInputStream();
			 
		// Set the command that you want to execute
		// In our case its the remote shell script
		channelExec.setCommand(command);
			 
		// Execute the command
		channelExec.connect();
		
		// Read the output from the input stream we set above
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
			      
		List<String> result = new ArrayList<>();
		//Read each line from the buffered reader and add it to result list
		// You can also simple print the result here
		while ((line = reader.readLine()) != null)
		{
			result.add(line);
		}
		
		System.out.println(result);
			 
		//retrieve the exit status of the remote command corresponding to this channel
		int exitStatus = channelExec.getExitStatus();
			 
		//Safely disconnect channel and disconnect session. If not done then it may cause resource leak
		channelExec.disconnect();
		session.disconnect();
			 
		if(exitStatus < 0){
			return true;
		} else if(exitStatus > 0){
			return false;
		} else{
			return true;
		}
	}

}
