package exchange.Clients.http;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import java.nio.charset.Charset;

/*
 * Author: Schliep
 */

public class NettyHttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpResponse msg) throws Exception {
        if(msg instanceof FullHttpResponse){
            FullHttpResponse response = (FullHttpResponse) msg;
            System.out.println(response.content().toString(Charset.defaultCharset()));
            channelHandlerContext.close();
        }
    }
    
}
