package org.example.apache.hc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.methods.HttpGet;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientSingletonLoadTest {
	private static final Logger logger = LoggerFactory.getLogger(HttpClientSingletonLoadTest.class);
	
	static final int REQ_COUNT = 1000000;
	static final int THREAD_COUNT = 200;

	@Test
	@Ignore
	public void testLoad() {
		CountDownLatch latch = new CountDownLatch(REQ_COUNT);
		String[] urisToGet = { 
				//"http://localhost/",
				"http://192.168.0.91:8080/spring_rest/getUserNoDBAction",
				"http://192.168.0.91:8080/spring_rest2/getUserNoDBAction"
		};

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		java.util.Date start = new java.util.Date();
		for (int i = 0; i < REQ_COUNT; i++) {
			Runnable worker = new MyTask(urisToGet[i % urisToGet.length], latch);
			executor.execute(worker);
		}

		try {
			latch.await();
		} catch (InterruptedException e) {
			logger.warn("",e);
		}
		java.util.Date end = new java.util.Date();
		
		logger.info(REQ_COUNT * 1000L / (end.getTime() - start.getTime()) + "");
		
		executor.shutdown();
		while (!executor.isTerminated()) {
		}

	}

	static class MyTask implements Runnable {
		private CountDownLatch latch;
		private String url;

		public MyTask(String url, CountDownLatch latch) {
			this.url = url;
			this.latch = latch;
		}

		@Override
		public void run() {
			HttpGet httpget = new HttpGet(this.url);
			try {
				HttpClientSingletonDemoTest.demo(httpget);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				this.latch.countDown();
			}
		}

		@Override
		public String toString() {
			return this.url;
		}
	}
}
