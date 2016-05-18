package schliep.bitcoin.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.AttributeKey;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;

public class HttpClient {

    private final Bootstrap bootstrap;
    private final EventLoopGroup group;

    public HttpClient() throws SSLException {
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


    public static void main(String[] args) throws SSLException {
        HttpClient client = new HttpClient();

        try {
            client.get("http://google.com");
            client.get("http://example.com");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            client.shutdown();
        }

    }

    private class HttpClientInitializer extends ChannelInitializer<SocketChannel>{

        private static final int MAX_LENGTH = 1048576;

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {

            ChannelPipeline pipeline = channel.pipeline();

            pipeline.addLast(new HttpClientCodec());

            pipeline.addLast(new HttpContentDecompressor());

            pipeline.addLast(new HttpObjectAggregator(MAX_LENGTH));

            pipeline.addLast(new HttpClientHandler());

        }
    }

}
