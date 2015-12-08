package exchange.Clients.websocket;

import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import exchange.Parsers.Parser;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

public class EchoWebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private Parser parser = null;

    public EchoWebSocketHandler(String URL, Parser p) throws URISyntaxException {
        URI uri = new URI(URL);
        this.parser = p;
        handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri,
                WebSocketVersion.V13, null, false, new DefaultHttpHeaders());
    }

    public ChannelFuture handshakeFuture(){
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx){
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx){
        System.out.println("WebSocket Client disconnected!");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();

        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            //System.out.println("WebSocket Client connected!");
            handshakeFuture.setSuccess();
            return;
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            parser.parse(textFrame.text());
            //System.out.println("WebSocket Client received message: " + textFrame.text());
        } else if (frame instanceof PongWebSocketFrame) {
            System.out.println("WebSocket Client received pong");
        } else if (frame instanceof CloseWebSocketFrame) {
            System.out.println("WebSocket Client received closing");
            ch.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        if(!handshakeFuture.isDone()){
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }
}
