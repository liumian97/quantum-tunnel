package win.liumian.qt.client.handler;

import io.netty.channel.Channel;
import win.liumian.qt.common.QuantumMessage;
import win.liumian.qt.common.QuantumMessageType;
import win.liumian.qt.common.handler.QuantumCommonHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liumian  2021/9/26 17:13
 */
@Slf4j
public class ProxyRequestHandler extends QuantumCommonHandler {

    final static Map<String, Channel> user2ProxyChannelMap = new ConcurrentHashMap<>();

    private final ChannelHandlerContext proxyChannelContext;

    private final String userChannelId;


    public ProxyRequestHandler(ChannelHandlerContext proxyChannelContext, String userChannelId) {
        this.proxyChannelContext = proxyChannelContext;
        this.userChannelId = userChannelId;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        user2ProxyChannelMap.put(userChannelId, channel);
        log.info("准备发起请求：" + channel.id().asLongText() + "，用户通道：" + userChannelId);
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        processData(ByteBufUtil.getBytes((ByteBuf) msg));
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
        message.setMessageType(QuantumMessageType.PROXY_DISCONNECTED);
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
