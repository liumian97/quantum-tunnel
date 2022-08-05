package top.liumian.qt.handler;

import com.google.protobuf.ByteString;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import top.liumian.qt.channel.ChannelMap;
import top.liumian.qt.common.handler.QuantumCommonHandler;
import top.liumian.qt.common.proto.QuantumMessage;

import java.nio.charset.StandardCharsets;

/**
 * @author liumian  2021/9/19 19:10
 */
@Slf4j
public class ProxyServerHandler extends QuantumCommonHandler {


    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (networkId != null && ChannelMap.PROXY_CHANNEL_MAP.containsKey(networkId)) {
            ChannelMap.PROXY_CHANNEL_MAP.remove(networkId);
            log.info("量子通道断开，网络id：{}", networkId);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        QuantumMessage.Message message = (QuantumMessage.Message) msg;
        if (message.getMessageType() == QuantumMessage.MessageType.REGISTER) {
            processRegister(ctx, message);
        } else if (message.getMessageType() == QuantumMessage.MessageType.PROXY_DISCONNECTED) {
            processProxyDisconnected(message);
        } else if (message.getMessageType() == QuantumMessage.MessageType.KEEPALIVE) {
            log.info("收到心跳消息，网络id：{}", message.getNetworkId());
        } else if (message.getMessageType() == QuantumMessage.MessageType.DATA) {
            writeToUserChannel(message);
        } else {
            ctx.channel().close();
            throw new RuntimeException("Unknown MessageType: " + message.getMessageType());
        }
    }

    /**
     * 处理内网穿透客户端注册事件
     *
     * @param ctx            上下文
     * @param quantumMessage 注册事件消息
     */
    private void processRegister(ChannelHandlerContext ctx, QuantumMessage.Message quantumMessage) {
        String serverVersion = System.getProperty("git.branch");
        Channel channel = ctx.channel();
        String networkId = quantumMessage.getNetworkId();
        QuantumMessage.Message.Builder builder = QuantumMessage.Message.newBuilder().setNetworkId(networkId);
        if (ChannelMap.PROXY_CHANNEL_MAP.containsKey(networkId)) {
            builder.setMessageType(QuantumMessage.MessageType.REGISTER_FAILED)
                    .setData(ByteString.copyFrom("重复注册，服务器分支版本：" + serverVersion, StandardCharsets.UTF_8));
            log.info("量子通道注册失败，网络id：{} 重复注册", networkId);
        } else {
            super.networkId = networkId;
            ChannelMap.PROXY_CHANNEL_MAP.put(networkId, channel);
            builder.setMessageType(QuantumMessage.MessageType.REGISTER_SUCCESS)
                    .setData(ByteString.copyFrom("注册成功，服务器分支版本：" + serverVersion, StandardCharsets.UTF_8));
            log.info("量子通道注册成功，网络id:{}", networkId);
        }
        QuantumMessage.Message message = builder.build();
        channel.writeAndFlush(message);
    }

    /**
     * 处理内网穿透客户端与目标服务器连接断开事件
     *
     * @param quantumMessage 断开事件消息
     */
    private void processProxyDisconnected(QuantumMessage.Message quantumMessage) {
        String channelId = quantumMessage.getChannelId();
        Channel channel = ChannelMap.USER_CHANNEL_MAP.get(channelId);
        if (channel != null && channel.isOpen()) {
            log.info("ProxyClient主动关闭用户通道，channelId:" + channelId);
            channel.close();
        }
    }

    /**
     * 向用户channel中发送真实服务器接收到的数据
     *
     * @param quantumMessage 数据事件消息
     */
    private void writeToUserChannel(QuantumMessage.Message quantumMessage) {
        Channel userChannel = ChannelMap.USER_CHANNEL_MAP.get(quantumMessage.getChannelId());
        if (userChannel != null) {
            userChannel.writeAndFlush(quantumMessage.getData().toByteArray());
        }
    }
}
