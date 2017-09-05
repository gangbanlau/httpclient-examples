package org.example.apache.hc;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientUtilTest {
	private static final Logger logger = LoggerFactory.getLogger(HttpClientUtilTest.class);
	
	@Test
	public void testGetHttpClient() throws IOException {
		logger.info("Execute testGetHttpClient");
		CloseableHttpClient httpClient = HttpClientUtil.getHttpClient();
		Assert.assertNotNull(httpClient);
		Assert.assertEquals(true, httpClient instanceof CloseableHttpClient);
		
		httpClient.close();
	}
	
	@Test
	public void testHTTPGet() throws ClientProtocolException, IOException {		
		CloseableHttpClient httpclient = HttpClientUtil.getHttpClient();

		try {
			HttpGet httpget = new HttpGet("http://www.apache.org/");

			logger.info("Executing request " + httpget.getRequestLine());

			CloseableHttpResponse response = httpclient.execute(httpget);
			try {
				System.out.println("----------------------------------------");
				System.out.println(response.getStatusLine());

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
						throw ex;
					} finally {
						// Closing the input stream will trigger connection
						// release
						instream.close();
					}
				}
			} finally {
				response.close();
			}
		} finally {
			try {
				httpclient.close();
			} catch (IOException e) {
			}
		}

	}
	
	@Test
	public void testHTTPSGet() throws ClientProtocolException, IOException {
		// JDK 7 try-with-resources statement
		try (CloseableHttpClient httpclient = HttpClientUtil.getHttpClient()) {
			
			HttpGet httpget = new HttpGet("https://www.google.org/");

			logger.info("Executing request " + httpget.getRequestLine());

			// Create a custom response handler
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

				@Override
				public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity) : null;
					} else {
						throw new ClientProtocolException("Unexpected response status: " + status);
					}
				}

			};
			String responseBody = httpclient.execute(httpget, responseHandler);
			// logger.info("----------------------------------------");
			// logger.info(responseBody);
			// TODO ASSERT something
		}
	}
}
