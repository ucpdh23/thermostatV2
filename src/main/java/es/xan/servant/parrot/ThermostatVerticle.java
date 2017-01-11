package es.xan.servant.parrot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import es.xan.servant.AbstractVerticle;

public class ThermostatVerticle extends AbstractVerticle implements Handler<Message<String>> {

	private CloseableHttpClient httpclient;

	private Logger logger;
	
	private boolean boilerOn;

	private static final String CONFIG = "sparkIo.api";
	private JsonObject configuration;
	
	@Override
	public void start() {
		logger = container.logger();
		configuration = container.config().getObject(CONFIG);

		httpclient = HttpClients.createDefault();
		
		vertx.eventBus().registerHandler(Constant.THERMOSTAT_VERTICLE, this);
		
		try {
			createScheduler("0 30 22 * * ?", id -> { 
				if (ThermostatVerticle.this.boilerOn) {
					vertx.eventBus().publish(Constant.THERMOSTAT_VERTICLE, "off");
				}
				});
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		boilerOn = false;
		
		logger.debug("started Thermostat");
	}

	@Override
	public void handle(Message<String> event) {
		final String operation = event.body();
		
		try {
			if (operation.equals("on")) {
				send("HIGH");
				boilerOn = true;
				event.reply(Constant.OK_MESSAGE);
			} else if (operation.equals("off")) {
				send("LOW");
				boilerOn = false;
				event.reply(Constant.OK_MESSAGE);
			} else if (operation.equals("status")) {
				event.reply(boilerOn? "boiler.on":"boiler.off");
			}
		} catch (Exception e) {
			e.printStackTrace();
			event.reply(Constant.KO_MESSAGE);
		}
	}

	private void send(String operation) throws ClientProtocolException, IOException {
		logger.info("setting thermostat to " + operation);
		
		final String url = configuration.getString("url");
		final String token = configuration.getString("token");
		
		final HttpPost httpPost = new HttpPost(url);
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("access_token", token));
		nvps.add(new BasicNameValuePair("params", "r4," + operation));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));
		CloseableHttpResponse response2 = httpclient.execute(httpPost);

		try {
		    System.out.println(response2.getStatusLine());
		    HttpEntity entity2 = response2.getEntity();
		    // do something useful with the response body
		    
		    // and ensure it is fully consumed
		    EntityUtils.consume(entity2);
		} finally {
		    response2.close();
		}
	}

}
