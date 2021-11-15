package top.liumian.qt.client.handler;

import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import top.liumian.qt.common.handler.QuantumCommonHandler;
import top.liumian.qt.common.proto.QuantumMessage;

/**
 * @author liumian  2021/9/26 17:13
 */
@Slf4j
public class ProxyRequestHandler extends QuantumCommonHandler {


    private final ChannelHandlerContext proxyChannelContext;

    private final String userChannelId;

    public ProxyRequestHandler(ChannelHandlerContext proxyChannelContext, String userChannelId, String networkId) {
        this.proxyChannelContext = proxyChannelContext;
        this.userChannelId = userChannelId;
        super.networkId = networkId;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        ProxyClientHandler.user2ProxyChannelMap.put(userChannelId, channel);
        log.info("准备发起请求，用户通道：" + userChannelId);
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        writeToUserChannel(ByteBufUtil.getBytes((ByteBuf) msg));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("主动断开连接，用户通道：" + userChannelId);
        processDisconnected();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("请求异常", cause);
//        cause.printStackTrace();
    }

    private void processDisconnected() {
        QuantumMessage.Message message = QuantumMessage.Message.newBuilder()
                .setNetworkId(networkId).setChannelId(userChannelId)
                .setMessageType(QuantumMessage.MessageType.PROXY_DISCONNECTED).build();
        proxyChannelContext.writeAndFlush(message);
    }


    private void writeToUserChannel(byte[] data) {
        QuantumMessage.Message message = QuantumMessage.Message.newBuilder()
                .setNetworkId(networkId).setChannelId(userChannelId)
                .setMessageType(QuantumMessage.MessageType.DATA).setData(ByteString.copyFrom(data)).build();
        proxyChannelContext.writeAndFlush(message);
    }

}
