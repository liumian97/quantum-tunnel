package win.liumian.qt.udp.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import win.liumian.qt.common.proto.QuantumMessage;

import java.nio.charset.StandardCharsets;


/**
 * @author liumian  2021/10/24 16:32
 */
@Slf4j
public class UdpEchoServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        byte[] bytes = ByteBufUtil.getBytes(msg.content());
        QuantumMessage.Message quantumMessage = QuantumMessage.Message.parseFrom(bytes);
        log.info("{} 接收到 {} 消息：{}", msg.recipient(), msg.sender(), quantumMessage.toString());
        ByteBuf byteBuf2Pre = Unpooled.copiedBuffer(quantumMessage.toByteArray());
        DatagramPacket packet = new DatagramPacket(byteBuf2Pre, msg.sender());
        ctx.channel().writeAndFlush(packet);
    }



}
