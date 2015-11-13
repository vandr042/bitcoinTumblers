package schliep.bitcoin.websockt;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import schliep.bitcoin.http.HttpClientHandler;

import java.net.URI;
import java.net.URISyntaxException;

public class WebsocketClient {

    private final Bootstrap bootstrap;
    private final EventLoopGroup group;

    public WebsocketClient(){
        bootstrap = new Bootstrap();

        group = new NioEventLoopGroup();

        //bootstrap.group(group).channel(NioSocketChannel.class).handler(new HttpClientInitializer());
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


    public static void main(String[] args){
        WebsocketClient client = new WebsocketClient();

        String url = "http://www.google.com/";

        try {
            client.get(url);
            client.get("http://reddit.com");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            client.shutdown();
        }

    }

    private class WebsocketClientInitializer extends ChannelInitializer<SocketChannel>{

        //private final WebsocketHandler handler;

        private WebsocketClientInitializer() {

            //WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.new;

            //handler = null;
        }


        @Override
        protected void initChannel(SocketChannel channel) throws Exception {

            ChannelPipeline pipeline = channel.pipeline();

            pipeline.addLast(new HttpClientCodec());

            pipeline.addLast(new HttpContentDecompressor());

            pipeline.addLast(new HttpClientHandler());

        }
    }

}
