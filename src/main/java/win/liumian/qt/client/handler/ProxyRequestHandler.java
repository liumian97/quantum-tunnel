package win.liumian.qt.client.handler;

import win.liumian.qt.common.QuantumMessage;
import win.liumian.qt.common.QuantumMessageType;
import win.liumian.qt.common.handler.QuantumCommonHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author liumian  2021/9/26 17:13
 */
@Slf4j
public class ProxyRequestHandler extends QuantumCommonHandler {


    private final ChannelHandlerContext proxyChannelContext;

    private final String userChannelId;


    public ProxyRequestHandler(ChannelHandlerContext proxyChannelContext, String userChannelId) {
        this.proxyChannelContext = proxyChannelContext;
        this.userChannelId = userChannelId;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        log.info("准备发起请求：" + ctx.channel().id().asLongText() + "，用户通道：" + userChannelId);
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpResponse httpResponse = (FullHttpResponse) msg;
//        log.info(ctx.channel().id().asLongText() + "：接收到消息：" + new String(data));

        EmbeddedChannel ch = new EmbeddedChannel(new HttpResponseEncoder());
        ch.writeOutbound(msg);
        ByteBuf encoded = ch.readOutbound();

        ch.close();

        processData(ByteBufUtil.getBytes(encoded));
//        ctx.channel().close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("准备断开连接：" + ctx.channel().id().asLongText() + "，用户通道：" + userChannelId);
        processDisconnected();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
    }

    private void processDisconnected() {
        QuantumMessage message = new QuantumMessage();
        message.setChannelId(userChannelId);
        message.setMessageType(QuantumMessageType.DISCONNECTED);
        proxyChannelContext.writeAndFlush(message);
    }


    private void processData(byte[] data) {
        QuantumMessage message = new QuantumMessage();
        message.setChannelId(userChannelId);
        message.setData(data);
        message.setMessageType(QuantumMessageType.DATA);
        proxyChannelContext.writeAndFlush(message);
    }

}
