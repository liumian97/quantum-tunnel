package win.liumian.qt.handler;

import com.alibaba.fastjson.JSONObject;
import win.liumian.qt.common.QuantumMessage;
import win.liumian.qt.common.QuantumMessageType;
import win.liumian.qt.common.handler.QuantumCommonHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import win.liumian.qt.channel.ChannelMap;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author liumian  2021/9/19 19:10
 */
@Slf4j
public class ProxyServerHandler extends QuantumCommonHandler {

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (networkId != null && ChannelMap.proxyChannelsMap.containsKey(networkId)) {
            ChannelMap.proxyChannelsMap.remove(networkId);
            log.info("量子通道断开，网络id：{}", networkId);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        QuantumMessage message = (QuantumMessage) msg;
        if (message.getMessageType() == QuantumMessageType.REGISTER) {
            processRegister(ctx, message);
        } else if (message.getMessageType() == QuantumMessageType.PROXY_DISCONNECTED) {
            processProxyDisconnected(message);
        } else if (message.getMessageType() == QuantumMessageType.KEEPALIVE) {
            log.info("收到心跳消息，网络id：{}", message.getNetworkId());
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
        JSONObject resultData = new JSONObject();
        QuantumMessage resultMsg = new QuantumMessage();
        resultMsg.setNetworkId(networkId);
        resultMsg.setMessageType(QuantumMessageType.REGISTER_RESULT);
        if (ChannelMap.proxyChannelsMap.containsKey(networkId)) {
            resultData.put("success", false);
            resultData.put("msg", "重复注册");
            log.info("量子通道注册失败，网络id：{} 重复注册", networkId);
        } else {
            super.networkId = networkId;
            ChannelMap.proxyChannelsMap.put(networkId, channel);
            resultData.put("success", true);
            log.info("量子通道注册成功，网络id:{}", networkId);
        }
        resultMsg.setData(resultData.toString().getBytes(StandardCharsets.UTF_8));
        channel.writeAndFlush(resultMsg);
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
