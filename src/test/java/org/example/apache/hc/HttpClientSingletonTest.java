package org.example.apache.hc;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.junit.Assert;
import org.junit.Test;

public class HttpClientSingletonTest {

	@Test
	public void testSingleton() throws IOException {
		
		HttpClient httpClient = HttpClientSingleton.getHttpClientInstance();
		Assert.assertNotNull(httpClient);
		Assert.assertEquals(true, httpClient instanceof HttpClient);
		
		HttpClient httpClient2 = HttpClientSingleton.getHttpClientInstance();
		Assert.assertEquals(true, httpClient == httpClient2);
	}
}

