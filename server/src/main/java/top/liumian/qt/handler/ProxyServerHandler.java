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
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (networkId != null && ChannelMap.proxyChannelsMap.containsKey(networkId)) {
            ChannelMap.proxyChannelsMap.remove(networkId);
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

    private void processRegister(ChannelHandlerContext ctx, QuantumMessage.Message quantumMessage) {
        String serverVersion = System.getProperty("git.branch");
        Channel channel = ctx.channel();
        String networkId = quantumMessage.getNetworkId();
        QuantumMessage.Message.Builder builder = QuantumMessage.Message.newBuilder().setNetworkId(networkId);
        if (ChannelMap.proxyChannelsMap.containsKey(networkId)) {
            builder.setMessageType(QuantumMessage.MessageType.REGISTER_SUCCESS)
                    .setData(ByteString.copyFrom("重复注册，服务器分支版本：" + serverVersion, StandardCharsets.UTF_8));
            log.info("量子通道注册失败，网络id：{} 重复注册", networkId);
        } else {
            super.networkId = networkId;
            ChannelMap.proxyChannelsMap.put(networkId, channel);
            builder.setMessageType(QuantumMessage.MessageType.REGISTER_SUCCESS)
                    .setData(ByteString.copyFrom("注册成功，服务器分支版本：" + serverVersion, StandardCharsets.UTF_8));
            log.info("量子通道注册成功，网络id:{}", networkId);
        }
        QuantumMessage.Message message = builder.build();
        channel.writeAndFlush(message);
    }

    private void processProxyDisconnected(QuantumMessage.Message quantumMessage) {
        String channelId = quantumMessage.getChannelId();
        Channel channel = ChannelMap.userChannelMap.get(channelId);
        if (channel != null && channel.isOpen()) {
            log.info("ProxyClient主动关闭用户通道，channelId:" + channelId);
            channel.close();
        }
    }

    private void writeToUserChannel(QuantumMessage.Message quantumMessage) {
        Channel userChannel = ChannelMap.userChannelMap.get(quantumMessage.getChannelId());
        if (userChannel != null) {
            userChannel.writeAndFlush(quantumMessage.getData().toByteArray());
        }
    }
}
