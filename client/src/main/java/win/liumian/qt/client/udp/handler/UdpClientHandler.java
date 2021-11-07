package win.liumian.qt.client.udp.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import win.liumian.qt.common.proto.QuantumMessage;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author liumian  2021/10/24 14:48
 */
@Slf4j
public class UdpClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {


    private Executor executor = Executors.newFixedThreadPool(2);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        byte[] bytes = ByteBufUtil.getBytes(msg.content());

        QuantumMessage.Message quantumMessage = QuantumMessage.Message.parseFrom(bytes);
        log.info("{} 收到对端：{}，消息：{}", msg.recipient(), msg.sender(), quantumMessage);
        if (quantumMessage.getMessageType() == QuantumMessage.MessageType.REGISTER_SUCCESS) {
            processRegisterResult(ctx, quantumMessage);
        } else if (quantumMessage.getMessageType() == QuantumMessage.MessageType.PING) {
            String hostName = msg.sender().getHostName();
            int port = msg.sender().getPort();
            processPing(ctx, quantumMessage.getNetworkId(), hostName, port);
        } else if (quantumMessage.getMessageType() == QuantumMessage.MessageType.PONG) {
            String hostName = msg.sender().getHostName();
            int port = msg.sender().getPort();
            processPong(ctx, quantumMessage.getNetworkId(), hostName, port);
        } else {
            Thread.sleep(10000);
            ByteBuf byteBuf = Unpooled.copiedBuffer(quantumMessage.toByteArray());
            DatagramPacket packet = new DatagramPacket(byteBuf, msg.sender());
            ctx.channel().writeAndFlush(packet);
        }
    }


    private void processRegisterResult(ChannelHandlerContext ctx, QuantumMessage.Message quantumMessage) {
        if (StringUtil.isNullOrEmpty(quantumMessage.getTargetHost())) {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            QuantumMessage.Message message = QuantumMessage.Message.newBuilder().setNetworkId(quantumMessage.getNetworkId())
                    .setMessageType(QuantumMessage.MessageType.REGISTER).build();
            ByteBuf byteBuf = Unpooled.copiedBuffer(message.toByteArray());
            InetSocketAddress socketAddress = new InetSocketAddress("101.35.83.105", 9999);
            DatagramPacket packet = new DatagramPacket(byteBuf, socketAddress);
            ctx.channel().writeAndFlush(packet);
        } else {
            QuantumMessage.Message message = QuantumMessage.Message.newBuilder().setNetworkId(quantumMessage.getNetworkId())
                    .setMessageType(QuantumMessage.MessageType.PING).build();


            for (int i = 0; i < 10; i++) {
                executor.execute(() -> {
                    log.info("发送ping：{}:{}", quantumMessage.getTargetHost(), quantumMessage.getTargetPort());
                    ByteBuf byteBuf2Pre = Unpooled.copiedBuffer(message.toByteArray());
                    InetSocketAddress recipient = new InetSocketAddress(quantumMessage.getTargetHost(), quantumMessage.getTargetPort());
                    DatagramPacket packet2Pre = new DatagramPacket(byteBuf2Pre, recipient);
                    ctx.channel().writeAndFlush(packet2Pre);

//                    ByteBuf byteBuf2Server = Unpooled.copiedBuffer(JSONObject.toJSONString(pingMsg), CharsetUtil.UTF_8);
//                    InetSocketAddress recipient2Server = new InetSocketAddress("101.35.83.105", 10000);
//                    DatagramPacket packet2Server = new DatagramPacket(byteBuf2Server, recipient2Server);
//                    ctx.channel().writeAndFlush(packet2Server);
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    private void processPing(ChannelHandlerContext ctx, String networkId, String targetHost, int targetPort) {
        log.info("收到ping：{},{}:{}", networkId, targetHost, targetPort);

        QuantumMessage.Message pongMsg = QuantumMessage.Message.newBuilder().setNetworkId(networkId).setMessageType(QuantumMessage.MessageType.PONG).build();

        ByteBuf byteBuf2Pre = Unpooled.copiedBuffer(pongMsg.toByteArray());
        InetSocketAddress recipient = new InetSocketAddress(targetHost, targetPort);
        DatagramPacket packet2Pre = new DatagramPacket(byteBuf2Pre, recipient);
        log.info("发送pong：{},{}:{}", networkId, targetHost, targetPort);
        ctx.channel().writeAndFlush(packet2Pre);
    }

    private void processPong(ChannelHandlerContext ctx, String networkId, String targetHost, int targetPort) {
        log.info("成功建立udp通道，网络id：{}，对端host：{}，对端port：{}", networkId, targetHost, targetPort);
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
