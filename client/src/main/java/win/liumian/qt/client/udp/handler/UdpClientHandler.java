package win.liumian.qt.client.udp.handler;

import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import win.liumian.qt.common.QuantumMessage;
import win.liumian.qt.common.QuantumMessageType;

import java.net.InetSocketAddress;

/**
 * @author liumian  2021/10/24 14:48
 */
@Slf4j
public class UdpClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        String response = msg.content().toString(CharsetUtil.UTF_8);
        log.info("{} 收到对端：{}，消息：{}", msg.recipient(), msg.sender(), response);
        QuantumMessage quantumMessage = JSONObject.parseObject(response, QuantumMessage.class);
        if (quantumMessage.getMessageType() == QuantumMessageType.REGISTER_RESULT) {
            processRegisterResult(ctx, quantumMessage);
        } else if (quantumMessage.getMessageType() == QuantumMessageType.PING) {
            String hostName = msg.sender().getHostName();
            int port = msg.sender().getPort();
            processPing(ctx, quantumMessage.getNetworkId(), hostName, port);
        } else if (quantumMessage.getMessageType() == QuantumMessageType.PONG) {
            String hostName = msg.sender().getHostName();
            int port = msg.sender().getPort();
            processPing(ctx, quantumMessage.getNetworkId(), hostName, port);
        }
    }


    private void processRegisterResult(ChannelHandlerContext ctx, QuantumMessage quantumMessage) {
        QuantumMessage pingMsg = new QuantumMessage();
        pingMsg.setNetworkId(quantumMessage.getNetworkId());
        pingMsg.setMessageType(QuantumMessageType.PING);

        ByteBuf byteBuf2Pre = Unpooled.copiedBuffer(JSONObject.toJSONString(pingMsg), CharsetUtil.UTF_8);
        InetSocketAddress recipient = new InetSocketAddress(pingMsg.getTargetHost(), pingMsg.getTargetPort());
        DatagramPacket packet2Pre = new DatagramPacket(byteBuf2Pre, recipient);
        ctx.channel().writeAndFlush(packet2Pre);
    }

    private void processPing(ChannelHandlerContext ctx, String networkId, String targetHost, int targetPort) {
        log.info("成功建立udp通道，网络id：{}，对端host：{}，对端port：{}", networkId, targetHost, targetPort);
    }

    private void processPong(ChannelHandlerContext ctx, String networkId, String targetHost, int targetPort) {
        QuantumMessage pongMsg = new QuantumMessage();
        pongMsg.setNetworkId(networkId);
        pongMsg.setMessageType(QuantumMessageType.PONG);

        ByteBuf byteBuf2Pre = Unpooled.copiedBuffer(JSONObject.toJSONString(pongMsg), CharsetUtil.UTF_8);
        InetSocketAddress recipient = new InetSocketAddress(targetHost, targetPort);
        DatagramPacket packet2Pre = new DatagramPacket(byteBuf2Pre, recipient);
        ctx.channel().writeAndFlush(packet2Pre);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("{} 连接关闭：{}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        log.info("{} 连接异常关闭：{}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        ctx.close();
    }
}
