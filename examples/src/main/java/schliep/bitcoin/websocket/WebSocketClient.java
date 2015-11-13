package schliep.bitcoin.websocket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketClient {

    private final Bootstrap bootstrap;
    private final EventLoopGroup group;

    private Channel channel;

    private URI uri;

    private EchoWebSocketHandler echoHandler = new EchoWebSocketHandler();

    public WebSocketClient() throws URISyntaxException, SSLException {
        uri = new URI(EchoWebSocketHandler.ECHO_URI);

        bootstrap = new Bootstrap();

        group = new NioEventLoopGroup();

        bootstrap.group(group).channel(NioSocketChannel.class).handler(new EchoWebSocketClientInitializer(uri, echoHandler));
    }

    public void connect() throws URISyntaxException, InterruptedException {
        String host = uri.getHost();
        int port = uri.getPort();
        if(port < 0){
            port = 443;
        }

        channel = bootstrap.connect(host, port).sync().channel();

        echoHandler.handshakeFuture().sync();
    }

    public void send(String text){
        WebSocketFrame frame = new TextWebSocketFrame("testing...");
        System.out.printf("Sending: %s\n", text);
        channel.writeAndFlush(frame);

    }

    public void shutdown(){
        group.shutdownGracefully();
    }


    public static void main(String[] args) throws URISyntaxException, SSLException {
        WebSocketClient client = new WebSocketClient();

        try {
            client.connect();
            client.send("testing...");
            Thread.sleep(1000);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            client.shutdown();
        }

    }

    private class EchoWebSocketClientInitializer extends ChannelInitializer<SocketChannel>{

        private static final int MAX_LENGTH = 1048576;

        private final EchoWebSocketHandler handler;
        private final String host;
        private int port;
        private final SslContext sslCtx;

        public EchoWebSocketClientInitializer(URI uri, EchoWebSocketHandler handler) throws URISyntaxException, SSLException {
            this.handler = handler;
            this.sslCtx = SslContextBuilder.forClient().build();

            host = uri.getHost();
            port = uri.getPort();
            if(port < 0){
                port = 443;
            }
        }


        @Override
        protected void initChannel(SocketChannel channel) throws Exception {

            ChannelPipeline pipeline = channel.pipeline();

            SslContext sslCtx = SslContextBuilder.forClient().build();

            pipeline.addLast(sslCtx.newHandler(channel.alloc(), host, port));

            pipeline.addLast(new HttpClientCodec());

            pipeline.addLast(new HttpContentDecompressor());

            pipeline.addLast(new HttpObjectAggregator(MAX_LENGTH));

            pipeline.addLast(handler);
        }
    }

}
