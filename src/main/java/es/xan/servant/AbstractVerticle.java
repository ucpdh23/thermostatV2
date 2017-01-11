package es.xan.servant;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.vertx.java.platform.Verticle;

public abstract class AbstractVerticle extends Verticle {
	private Map<Long, Long> mVertxIdManager = new HashMap<>();
	
	/**
	 * 
	 * @param cronExpression in GMT
	 * @param consumer
	 * @return
	 * @throws Exception
	 */
	protected long createScheduler(String cronExpression, Consumer<Long> consumer) throws Exception {
		final long myId = new Random().nextLong();
		
		createScheduler(myId, cronExpression, consumer);
		
		return myId;
	}

	private void createScheduler(Long myId, String cronExpression, Consumer<Long> consumer) throws Exception {
		long delay = convertInDelay(cronExpression);
		
		long vertxId = vertx.setTimer(delay * 1000,
				id -> {
					try {
						consumer.accept(id);
						createScheduler(myId, cronExpression, consumer);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
		
		mVertxIdManager.put(myId, vertxId);
		
		
	}
	
	public static final void main(String args[]) throws Exception {
		String cronExpression = "0 10 17 11 1 ?";
		System.out.println(convertInDelay(cronExpression));
	}

	private static long convertInDelay(String cronExpression) throws Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(new URI("http","www.cronmaker.com", "/rest/sampler", "expression=" + cronExpression, null));

		HttpContext localContext = new BasicHttpContext();

		CloseableHttpResponse response = httpclient.execute(httpGet, localContext);
		
		String content = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		
		for (int i=0; i < 5; i++) {
			String nextTimestamp = content.split(",")[i];
			
			DateTimeFormatter f = DateTimeFormatter.ISO_DATE_TIME;
			ZonedDateTime zonedNextScheduled = ZonedDateTime.parse(nextTimestamp, f);
			
			final LocalDateTime localNow = LocalDateTime.now();
			final ZoneId currentZone = ZoneId.of("Europe/Madrid");
			final ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
			
	        if(zonedNow.compareTo(zonedNextScheduled) > 0)
	        	continue;
			
			final Duration duration = Duration.between(zonedNow, zonedNextScheduled);
			final long initialDelay = duration.getSeconds();
			
			return initialDelay;
		}
		
		throw new Exception();
	}

}
