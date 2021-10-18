package win.liumian.qt.server.handler;

import win.liumian.qt.common.QuantumMessage;
import win.liumian.qt.common.QuantumMessageType;
import win.liumian.qt.common.handler.QuantumCommonHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import win.liumian.qt.server.channel.ChannelMap;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author liumian  2021/9/19 19:10
 */
@Slf4j
public class ProxyServerHandler extends QuantumCommonHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        QuantumMessage message = (QuantumMessage) msg;
        if (message.getMessageType() == QuantumMessageType.REGISTER) {
            processRegister(ctx, message);
        } else if (message.getMessageType() == QuantumMessageType.PROXY_DISCONNECTED) {
            processProxyDisconnected(message);
        } else if (message.getMessageType() == QuantumMessageType.DATA) {
            processData(message);
        } else {
            ctx.channel().close();
            throw new RuntimeException("Unknown MessageType: " + message.getMessageType());
        }
    }

    private void processRegister(ChannelHandlerContext ctx, QuantumMessage quantumMessage) {
        Channel channel = ctx.channel();
        String networkId = quantumMessage.getNetworkId();
        ChannelMap.proxyChannelsMap.put(networkId, channel);
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("success", true);
        QuantumMessage resultMsg = new QuantumMessage();
        resultMsg.setNetworkId(networkId);
        resultMsg.setMessageType(QuantumMessageType.REGISTER_RESULT);
        resultMsg.setData(resultData.toString().getBytes(StandardCharsets.UTF_8));
        channel.writeAndFlush(resultMsg);
        log.info("量子通道注册成功，网络id:{}",networkId);
    }

    private void processProxyDisconnected(QuantumMessage quantumMessage) {
        String channelId = quantumMessage.getChannelId();
        Channel channel = ChannelMap.userChannelMap.get(channelId);
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        log.info("用户通道关闭，channelId:" + channelId);
    }

    private void processData(QuantumMessage quantumMessage) {
        Channel userChannel = ChannelMap.userChannelMap.get(quantumMessage.getChannelId());
        if (userChannel != null) {
            userChannel.writeAndFlush(quantumMessage.getData());
        }
    }
}
