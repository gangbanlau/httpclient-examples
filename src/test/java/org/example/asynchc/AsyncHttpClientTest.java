package org.example.asynchc;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;

public class AsyncHttpClientTest {
	private static final Logger logger = LoggerFactory.getLogger(AsyncHttpClientTest.class);
	
	public static final String TARGET_URL = "http://www.apache.org/";
	
	@Test
	public void testFuture() throws InterruptedException, ExecutionException, IOException {		
		AsyncHttpClient client = new DefaultAsyncHttpClient();
		Future<Response> f = client.prepareGet(TARGET_URL).execute();
		
		logger.info("do other things");
		Thread.sleep(3000);
		
		logger.info("get response");
		Response r = f.get();
		logger.info("Response: " + r.getStatusCode() + " " + r.getStatusText());
		
		client.close();
	}
	
	@Test
	public void testAsyncCompletionHandler() throws InterruptedException, IOException {
		AsyncHttpClientConfig cf = new DefaultAsyncHttpClientConfig.Builder()
				.setConnectTimeout(3000).build();
			    //.setProxyServer(new ProxyServer.Builder("127.0.0.1", 38080)).build();
		
		AsyncHttpClient client = new DefaultAsyncHttpClient(cf);
		CountDownLatch latch = new CountDownLatch(1);
		
		// (this will also fully read Response in memory before calling onCompleted)
		client.prepareGet(TARGET_URL).execute(new AsyncCompletionHandler<Response>(){
		    
		    @Override
		    public Response onCompleted(Response response) throws Exception{
		        // Do something with the Response
		        logger.info("onComplete: " + response.getStatusCode() + " " + response.getStatusText());
		        latch.countDown();
		        return response;
		    }
		    
		    @Override
		    public void onThrowable(Throwable t){
		        // Something wrong happened.
		    	logger.warn("", t);
		    	latch.countDown();
		    }
		});	
		
		latch.await();
		
		client.close();
	}
	
	@Test
	public void testPost() throws InterruptedException, IOException {
		AsyncHttpClient client = new DefaultAsyncHttpClient();
		CountDownLatch latch = new CountDownLatch(1);

		Map<String, List<String>> m = new HashMap<>();
		for (int i = 0; i < 5; i++) {
			m.put("param_" + i, Arrays.asList("值_" + i));
		}

		Request req = new RequestBuilder("POST").setUrl(TARGET_URL)
				.addHeader("Content-Type", "application/x-www-form-urlencoded").setFormParams(m).build();

		// (this will also fully read Response in memory before calling
		// onCompleted)
		client.executeRequest(req, new AsyncCompletionHandler<Response>() {

			@Override
			public Response onCompleted(Response response) throws Exception {
				// Do something with the Response
				logger.info("onComplete: " + response.getStatusCode() + " " + response.getStatusText());
				latch.countDown();
				return response;
			}

			@Override
			public void onThrowable(Throwable t) {
				// Something wrong happened.
				logger.warn("", t);
				latch.countDown();
			}
		});

		latch.await();

		client.close();
	}
	
	
/*	@Test
	public void testJava8() {
		AsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient();
		CompletableFuture<Response> promise = asyncHttpClient
		            .prepareGet(TARGET_URL)
		            .execute()
		            .toCompletableFuture()
		            .exceptionally((t, resp) -> {  Something wrong happened...   } )
		            .thenApply(resp -> {   Do something with the Response  return resp; });
		promise.join(); // wait for completion		
	}*/
}
