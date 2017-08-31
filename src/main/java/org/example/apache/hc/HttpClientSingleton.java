package org.example.apache.hc;

import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpClient 单例模式
 * 
 * 类似 HttpClientUtil，这里提供复用 HttpClient 实例的实现。也就是同一个 HttpClient 实例服务于多次 HTTP 操作。
 * 调用者获得 HttpClient 实例后，只能调用类似 In/Out stream 的 close() 方法或者 CloseableHttpResponse 的 close() 方法释放
 * 资源，不能调用 HttpClient 实例 的 close() 方法。
 * 
 * @author gang
 *
 */
public class HttpClientSingleton {

	private static final Logger logger = LoggerFactory.getLogger(HttpClientSingleton.class);
	
	private static HttpClientSingleton instance = null;

	// Lazy Initialization (If required then only)
	public static synchronized CloseableHttpClient getHttpClientInstance() {
		if (instance == null) {
			instance = new HttpClientSingleton();
		}
		return instance.getHttpClient();
	}

	private CloseableHttpClient hc = null;
	
	private HttpClientSingleton() {
		hc = HttpClientUtil.getHttpClient();
		logger.info("create httpclient instance successfully");
	}
 
	public CloseableHttpClient getHttpClient() {
		return hc;
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (hc != null) {
			hc.close();
			logger.info("close httpclient instance successfully");
			hc = null;
		}
		
		super.finalize();
	}
	
}
