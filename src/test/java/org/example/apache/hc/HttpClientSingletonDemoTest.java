package org.example.apache.hc;

import org.example.apache.hc.HttpClientSingletonMultithreadTest.LocalHttpSrv;
import org.example.apache.hc.HttpClientSingletonMultithreadTest.GetThread;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientSingletonDemoTest {
	private static final Logger logger = LoggerFactory.getLogger(HttpClientSingletonDemoTest.class);
	
	static final int PORT = HttpClientSingletonMultithreadTest.getRandomNumberInRange(10000, 20000);
	
	private static LocalHttpSrv srv = null;
	
	@Rule public ExpectedException thrown= ExpectedException.none();
	
	@BeforeClass
	public static void setup() {
		srv = new LocalHttpSrv(PORT);
		srv.start();
	}
	
	@AfterClass
	public static void shutdown() throws IOException {
		//httpClient.close();
		srv.close();
	}
	
	@Test
	public void testDemo() throws Exception {
		HttpGet httpget = new HttpGet("http://www.apache.org/");
		demo(httpget);
	}
	
	@Test
	public void testWrongURL() throws Exception {
		thrown.expect( ClientProtocolException.class );
		HttpGet httpget = new HttpGet("xxx://yyyy");
		demo(httpget);
	}
	
	@Test
	public void testConnectionReuse() throws Exception {
		logger.info("##### testConnectionReuse #####");
		HttpGet httpget = new HttpGet("http://localhost:" + PORT);
		demo(httpget);
		HttpGet httpget2 = new HttpGet("http://localhost:" + PORT);
		demo(httpget2);
		logger.info("##### testConnectionReuse end #####");
	}
	
	@Test
	public void testConnectionReuseConcurrent() throws Exception {
		logger.info("##### testConnectionReuseConcurrent #####");
		// create an array of URIs to perform GETs on
		String[] urisToGet = { "http://localhost:" + PORT };

		int threadCount = 10;
		GetThread[] threads = new GetThread[threadCount];

		// create a thread for each URI
		for (int i = 0; i < threads.length; i++) {
			HttpGet httpget = new HttpGet(urisToGet[i % urisToGet.length]);
			threads[i] = new GetThread(HttpClientSingleton.getHttpClientInstance(), httpget, i + 1);
		}

		// start the threads
		for (int j = 0; j < threads.length; j++) {
			threads[j].start();
		}

		// join the threads
		for (int j = 0; j < threads.length; j++) {
			threads[j].join();
		}
		logger.info("##### testConnectionReuseConcurrent end #####");
	}
	
	/**
	 * 复用 HttpClient Demo 代码
	 * 
	 * @param httpget
	 * @throws Exception
	 */
	public static void demo(HttpGet httpget) throws Exception {
		try {
			logger.debug("Executing request " + httpget.getRequestLine());
			CloseableHttpResponse response = HttpClientSingleton.getHttpClientInstance().execute(httpget);
			try {
				logger.debug("----------------------------------------");
				// System.out.println(response.getStatusLine());

				// Get hold of the response entity
				HttpEntity entity = response.getEntity();

				// If the response does not enclose an entity, there is no need
				// to bother about connection release
				if (entity != null) {
					InputStream instream = entity.getContent();
					try {
						instream.read();
						// do something useful with the response
					} catch (IOException ex) {
						// In case of an IOException the connection will be
						// released
						// back to the connection manager automatically
						logger.warn("", ex);
						throw ex;
					} finally {
						// Closing the input stream will trigger connection
						// release
						instream.close(); // !!! 注意此处 close
					}
				}
			} finally {
				response.close(); // !!! 注意此处 close
			}
		} finally {
			// httpClient.close(); // 因为我们需要重用 HttpClient instance，因此此处不能调用
			// close
		}
	}
}
