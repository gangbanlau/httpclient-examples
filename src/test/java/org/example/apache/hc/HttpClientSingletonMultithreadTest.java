package org.example.apache.hc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;  
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;  
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;  
import static io.netty.handler.codec.http.HttpResponseStatus.OK;  
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1; 

import java.io.IOException;
import java.util.Random;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientSingletonMultithreadTest {
	/**
	 * 本地 http server 返回数据
	 */
	static final String DATA = "I am ok";
	
	static final int PORT = getRandomNumberInRange(10000, 20000);
	
	/**
	 * 本地 http server 实现
	 */
	private static LocalHttpSrv srv = null;
	
	public static int getRandomNumberInRange(int min, int max) {

		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}

		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}
	
	@BeforeClass
	public static void setup() throws Exception {
		srv = new LocalHttpSrv(PORT);
		srv.start();
	}
	
	@AfterClass
	public static void shutdown() throws Exception {
		srv.close();
	}
	
	@Test
	public void testMultithread() throws IOException, InterruptedException {
        // create an array of URIs to perform GETs on
        String[] urisToGet = {
        		"http://localhost:" + PORT
        		//"http://192.168.0.91:8080/spring_rest/getUserNoDBAction"
        };

        int threadCount = 100;
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
	}
		
	/**
	 * A thread that performs a GET.
	 */
	public static class GetThread extends Thread {

	    private final CloseableHttpClient httpClient;
	    private final HttpContext context;
	    private final HttpGet httpget;
	    private final int id;

	    public GetThread(CloseableHttpClient httpClient, HttpGet httpget, int id) {
	        this.httpClient = httpClient;
	        this.context = new BasicHttpContext();
	        this.httpget = httpget;
	        this.id = id;
	    }

	    /**
	     * Executes the GetMethod and prints some status information.
	     */
	    @Override
	    public void run() {
	    	String resp = "";
	    	
	        try {
	            //System.out.println(id + " - about to get something from " + httpget.getURI());
	            CloseableHttpResponse response = httpClient.execute(httpget, context);
	            try {
	                //System.out.println(id + " - get executed");
	                // get the response body as an array of bytes
	                HttpEntity entity = response.getEntity();
	                if (entity != null) {
	                	byte[] bytes = EntityUtils.toByteArray(entity);
	                    //System.out.println(id + " - " + bytes.length + " bytes read");
	                	resp = new String(bytes);
	                }	            
	            } finally {
	                response.close();
	            }
	            
	            Assert.assertEquals(true, DATA.equals(resp));
	        } catch (Exception e) {
	            System.out.println(id + " - error: " + e);
	            Assert.assertEquals(true, 1 == 2);
	        }
	    }

	}	
	
    public static class LocalHttpSrv extends Thread {
    	HttpServer server;
    	int port;
    	
    	LocalHttpSrv(int port) {
    		server = new HttpServer();
    		this.port = port;
    	}
    	
    	public void run () {
    		try {
				server.start(this.port);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	public void close() {
    		server.shutdown();
    	}
    }
	
	static class HttpServer {  
		EventLoopGroup bossGroup;  
	    EventLoopGroup workerGroup;
	     
	    public HttpServer() {
	        bossGroup = new NioEventLoopGroup(); // (1)  
	        workerGroup = new NioEventLoopGroup();  	    	
	    }
	    
		public void shutdown() {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	    public void start(int port) throws Exception {  
	        try {  
	            ServerBootstrap b = new ServerBootstrap(); // (2)  
	            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class) // (3)  
	                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)  
	                                @Override  
	                                public void initChannel(SocketChannel ch) throws Exception {  
	                                    // server端发送的是httpResponse，所以要使用HttpResponseEncoder进行编码  
	                                    ch.pipeline().addLast(new HttpResponseEncoder());  
	                                    // server端接收到的是httpRequest，所以要使用HttpRequestDecoder进行解码  
	                                    ch.pipeline().addLast(new HttpRequestDecoder());  
	                                    ch.pipeline().addLast(new HttpServerInboundHandler());  
	                                }  
	                            }).option(ChannelOption.SO_BACKLOG, 128) // (5)  
	                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)  
	  
	            ChannelFuture f = b.bind(port).sync(); // (7)  
	  
	            f.channel().closeFuture().sync();  
	        } finally {  
	            workerGroup.shutdownGracefully();  
	            bossGroup.shutdownGracefully();  
	        }  
	    }  
	} 
	
	static class HttpServerInboundHandler extends ChannelInboundHandlerAdapter {  
	    private static Logger   logger  = LoggerFactory.getLogger(HttpServerInboundHandler.class);  
	    private ByteBufToBytes reader;  
	  
	    @Override  
	    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {  
	        if (msg instanceof HttpRequest) {  
	        	
	            HttpRequest request = (HttpRequest) msg;  
	            //System.out.println("messageType:" + request.headers().get("messageType"));  
	            //System.out.println("businessType:" + request.headers().get("businessType"));  
	            if (HttpHeaders.isContentLengthSet(request)) {  
	                reader = new ByteBufToBytes((int) HttpHeaders.getContentLength(request));  
	            } 
	             
	        }  
	  
	        if (msg instanceof HttpContent) {  
	            HttpContent httpContent = (HttpContent) msg;  
	            ByteBuf content = httpContent.content(); 
	            if (reader != null)
	            	reader.reading(content);  
	            content.release();  
	            
	            
	            if (reader == null || reader.isEnd()) {
	            	if (reader != null) {
	                String resultStr = new String(reader.readFull());  
	                System.out.println("Client said:" + resultStr);  
	            	}
	            	
	                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(DATA  
	                        .getBytes()));  
	                response.headers().set(CONTENT_TYPE, "text/plain");  
	                response.headers().set(CONTENT_LENGTH, response.content().readableBytes());  
	                response.headers().set(CONNECTION, Values.KEEP_ALIVE);  
	                ctx.write(response);  
	                ctx.flush();  
	            }  
	        }  
	    }  
	  
	    @Override  
	    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {  
	        logger.info("HttpServerInboundHandler.channelReadComplete");  
	        ctx.flush();  
	    }  
	  
	} 
	
	static class ByteBufToBytes {  
	    private ByteBuf temp;  
	  
	    private boolean end = true;  
	  
	    public ByteBufToBytes(int length) {  
	        temp = Unpooled.buffer(length);  
	    }  
	  
	    public void reading(ByteBuf datas) {  
	        datas.readBytes(temp, datas.readableBytes());  
	        if (this.temp.writableBytes() != 0) {  
	            end = false;  
	        } else {  
	            end = true;  
	        }  
	    }  
	  
	    public boolean isEnd() {  
	        return end;  
	    }  
	  
	    public byte[] readFull() {  
	        if (end) {  
	            byte[] contentByte = new byte[this.temp.readableBytes()];  
	            this.temp.readBytes(contentByte);  
	            this.temp.release();  
	            return contentByte;  
	        } else {  
	            return null;  
	        }  
	    }  
	  
	    public byte[] read(ByteBuf datas) {  
	        byte[] bytes = new byte[datas.readableBytes()];  
	        datas.readBytes(bytes);  
	        return bytes;  
	    }  
	}  
}
