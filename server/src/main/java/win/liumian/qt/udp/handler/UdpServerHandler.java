package win.liumian.qt.udp.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import win.liumian.qt.common.proto.QuantumMessage;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author liumian  2021/10/24 16:32
 */
@Slf4j
public class UdpServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private Map<String, InetSocketAddress> networkId2Addr = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        byte[] bytes = ByteBufUtil.getBytes(msg.content());

        QuantumMessage.Message quantumMessage = QuantumMessage.Message.parseFrom(bytes);
        log.info("{} 接收到 {} 消息：{}", msg.recipient(), msg.sender(), quantumMessage.toString());


        if (quantumMessage.getMessageType() == QuantumMessage.MessageType.REGISTER) {
            InetSocketAddress currentAddr = msg.sender();
            String networkId = quantumMessage.getNetworkId();
            if (networkId2Addr.containsKey(networkId)) {

                InetSocketAddress preAddr = networkId2Addr.get(networkId);
                if (preAddr.toString().equals(currentAddr.toString())) {
                    networkId2Addr.put(networkId, currentAddr);
                    ByteBuf byteBuf2Pre = Unpooled.copiedBuffer(newRegisterResultMsg(networkId, null, 0).toByteArray());
                    DatagramPacket packet2Pre = new DatagramPacket(byteBuf2Pre, msg.sender());
                    ctx.channel().writeAndFlush(packet2Pre);
                } else {
                    //往两个客户端发送注册结果，告知对方的ip和端口
                    log.info("通知互相ping，currentAddr：{}，preAddr：{}", currentAddr, preAddr);

                    ByteBuf byteBuf2Current = Unpooled.copiedBuffer(newRegisterResultMsg(networkId, preAddr.getHostName(), preAddr.getPort()).toByteArray());
                    DatagramPacket packet2Current = new DatagramPacket(byteBuf2Current, currentAddr);
                    ctx.channel().writeAndFlush(packet2Current);


                    ByteBuf byteBuf2Pre = Unpooled.copiedBuffer(newRegisterResultMsg(networkId, currentAddr.getHostName(), currentAddr.getPort()).toByteArray());
                    DatagramPacket packet2Pre = new DatagramPacket(byteBuf2Pre, preAddr);
                    ctx.channel().writeAndFlush(packet2Pre);

                    networkId2Addr.remove(networkId);
                }
            } else {
                networkId2Addr.put(networkId, currentAddr);
                ByteBuf byteBuf2Pre = Unpooled.copiedBuffer(newRegisterResultMsg(networkId, null, 0).toByteArray());
                DatagramPacket packet2Pre = new DatagramPacket(byteBuf2Pre, msg.sender());
                ctx.channel().writeAndFlush(packet2Pre);
            }
        }
    }


    private QuantumMessage.Message newRegisterResultMsg(String networkId, String remoteHost, int remotePort) {
        QuantumMessage.Message.Builder builder = QuantumMessage.Message.newBuilder().setNetworkId(networkId)
                .setMessageType(QuantumMessage.MessageType.REGISTER_SUCCESS).setTargetPort(remotePort);
        if (remoteHost != null) {
            builder.setTargetHost(remoteHost);
        }
        return builder.build();
    }


}
