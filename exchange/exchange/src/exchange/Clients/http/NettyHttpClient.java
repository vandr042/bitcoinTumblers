package exchange.Clients.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import java.net.*;

/*
 * Author: Schliep
 */


public class NettyHttpClient {
    
    private final Bootstrap bootstrap;
    private final EventLoopGroup group;
    
    public NettyHttpClient(){
        bootstrap = new Bootstrap();
        group = new NioEventLoopGroup();
        bootstrap.group(group).channel(NioSocketChannel.class).handler(new HttpClientInitializer());
    }
    
    public void get(String url) throws URISyntaxException, InterruptedException {
        URI uri = new URI(url);
        
        String host = uri.getHost();
        int port = uri.getPort();
        if(port < 0){
            port = 80;
        }

        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());

        request.headers().set(HttpHeaders.Names.HOST, host);
        request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);

        Channel ch = bootstrap.connect(host, port).sync().channel();
        ch.writeAndFlush(request);
        ch.closeFuture().sync(); 
    }
    
    public void shutdown(){
        group.shutdownGracefully();
    }
    
    
    private class HttpClientInitializer extends ChannelInitializer<SocketChannel>{

        private static final int MAX_LENGTH = 1048576;

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addLast(new HttpClientCodec());
            pipeline.addLast(new HttpContentDecompressor());
            pipeline.addLast(new HttpObjectAggregator(MAX_LENGTH));
            pipeline.addLast(new NettyHttpClientHandler());

        }
    }
    
}
