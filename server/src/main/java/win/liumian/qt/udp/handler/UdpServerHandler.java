package win.liumian.qt.udp.handler;

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
        String response = msg.content().toString(CharsetUtil.UTF_8);
        log.info("接收到 {} 消息：{}", msg.sender(), response);
        QuantumMessage quantumMessage = JSONObject.parseObject(response, QuantumMessage.class);

        if (quantumMessage.getMessageType() == QuantumMessageType.REGISTER) {
            InetSocketAddress currentAddr = msg.recipient();
            String networkId = quantumMessage.getNetworkId();
            if (networkId2Addr.containsKey(networkId)) {

                InetSocketAddress preAddr = networkId2Addr.get(networkId);
                if (preAddr.toString().equals(currentAddr.toString())) {
                    //相等则说明是同一个客户端，直接丢弃
                    return;
                } else {
                    //往两个客户端发送注册结果，告知对方的ip和端口

                    ByteBuf byteBuf2Current = Unpooled.copiedBuffer(JSONObject.toJSONString(newRegisterResultMsg(networkId, preAddr.getHostName(), preAddr.getPort())), CharsetUtil.UTF_8);
                    DatagramPacket packet2Current = new DatagramPacket(byteBuf2Current, currentAddr);
                    ctx.channel().writeAndFlush(packet2Current);


                    ByteBuf byteBuf2Pre = Unpooled.copiedBuffer(JSONObject.toJSONString(newRegisterResultMsg(networkId, currentAddr.getHostName(), currentAddr.getPort())), CharsetUtil.UTF_8);
                    DatagramPacket packet2Pre = new DatagramPacket(byteBuf2Pre, preAddr);
                    ctx.channel().writeAndFlush(packet2Pre);

                    networkId2Addr.remove(networkId);
                }
            } else {
                networkId2Addr.put(networkId, currentAddr);
            }
        }
    }


    private QuantumMessage newRegisterResultMsg(String networkId, String remoteHost, int remotePort) {
        QuantumMessage quantumMessage = new QuantumMessage();
        quantumMessage.setMessageType(QuantumMessageType.REGISTER_RESULT);
        quantumMessage.setNetworkId(networkId);
        quantumMessage.setTargetHost(remoteHost);
        quantumMessage.setTargetPort(remotePort);
        return quantumMessage;
    }


}
