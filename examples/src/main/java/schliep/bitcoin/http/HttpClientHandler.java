package schliep.bitcoin.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;

import java.nio.charset.Charset;

public class HttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpResponse msg) throws Exception {
        if(msg instanceof FullHttpResponse){
            FullHttpResponse response = (FullHttpResponse) msg;
            System.out.println(response.content().toString(Charset.defaultCharset()));
            channelHandlerContext.close();
        }
    }
}
