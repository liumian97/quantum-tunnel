package win.liumian.qt.handler;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import win.liumian.qt.channel.ChannelMap;
import win.liumian.qt.common.handler.QuantumCommonHandler;
import win.liumian.qt.common.proto.QuantumMessage;

import java.nio.charset.StandardCharsets;

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
        QuantumMessage.Message message = (QuantumMessage.Message) msg;
        if (message.getMessageType() == QuantumMessage.Message.MessageType.REGISTER) {
            processRegister(ctx, message);
        } else if (message.getMessageType() == QuantumMessage.Message.MessageType.PROXY_DISCONNECTED) {
            processProxyDisconnected(message);
        } else if (message.getMessageType() == QuantumMessage.Message.MessageType.KEEPALIVE) {
            log.info("收到心跳消息，网络id：{}", message.getNetworkId());
        } else if (message.getMessageType() == QuantumMessage.Message.MessageType.DATA) {
            writeToUserChannel(message);
        } else {
            ctx.channel().close();
            throw new RuntimeException("Unknown MessageType: " + message.getMessageType());
        }
    }

    private void processRegister(ChannelHandlerContext ctx, QuantumMessage.Message quantumMessage) {
        Channel channel = ctx.channel();
        String networkId = quantumMessage.getNetworkId();
        JSONObject resultData = new JSONObject();
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
        QuantumMessage.Message message = QuantumMessage.Message.newBuilder().setNetworkId(networkId)
                .setMessageType(QuantumMessage.Message.MessageType.REGISTER_RESULT)
                .setData(ByteString.copyFrom(resultData.toJSONString(), StandardCharsets.UTF_8)).build();
        channel.writeAndFlush(message);
    }

    private void processProxyDisconnected(QuantumMessage.Message quantumMessage) {
        String channelId = quantumMessage.getChannelId();
        Channel channel = ChannelMap.userChannelMap.get(channelId);
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        log.info("用户通道关闭，channelId:" + channelId);
    }

    private void writeToUserChannel(QuantumMessage.Message quantumMessage) {
        Channel userChannel = ChannelMap.userChannelMap.get(quantumMessage.getChannelId());
        if (userChannel != null) {
            userChannel.writeAndFlush(quantumMessage.getData().toByteArray());
        }
    }
}
