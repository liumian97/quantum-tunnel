package win.liumian.qt.server.handler;

import win.liumian.qt.common.QuantumMessage;
import win.liumian.qt.common.QuantumMessageType;
import win.liumian.qt.common.handler.QuantumCommonHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liumian  2021/9/19 19:10
 */
@Slf4j
public class ProxyServerHandler extends QuantumCommonHandler {

    public static final Map<String, Channel> proxyChannelsMap = new ConcurrentHashMap<>();

    private volatile boolean register = false;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        QuantumMessage message = (QuantumMessage) msg;
        if (message.getMessageType() == QuantumMessageType.REGISTER) {
            processRegister(ctx, message);
        } else if (register) {
            if (message.getMessageType() == QuantumMessageType.DISCONNECTED) {
                processDisconnected(message);
            } else if (message.getMessageType() == QuantumMessageType.DATA) {
                processData(message);
            } else if (message.getMessageType() == QuantumMessageType.KEEPALIVE) {
                // 心跳包, 不处理
                log.info("收到心跳包：" + message.getClientId());
            } else {
                throw new RuntimeException("Unknown type: " + message.getMessageType());
            }
        } else {
            ctx.channel().close();
        }
    }

    private void processRegister(ChannelHandlerContext ctx, QuantumMessage quantumMessage) {
        Channel channel = ctx.channel();
        String clientId = quantumMessage.getClientId();
        proxyChannelsMap.put(clientId, channel);
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("success", true);
        QuantumMessage resultMsg = new QuantumMessage();
        resultMsg.setClientId(clientId);
        resultMsg.setMessageType(QuantumMessageType.REGISTER_RESULT);
        resultMsg.setData(resultData.toString().getBytes(StandardCharsets.UTF_8));
        channel.writeAndFlush(resultMsg);
        register = true;
        log.info("quantum tunnel register success,clientId:" + clientId);
    }


    private void processDisconnected(QuantumMessage quantumMessage) {
        String channelId = quantumMessage.getChannelId();
        Channel channel = UserServerHandlerV2.userChannelMap.get(channelId);
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        log.info("quantum tunnel closed,channelId:" + channelId);
    }

    private void processData(QuantumMessage quantumMessage) {
        Channel userChannel = UserServerHandlerV2.userChannelMap.get(quantumMessage.getChannelId());
        if (userChannel != null) {
            userChannel.writeAndFlush(quantumMessage.getData());
//                    .addListener(ChannelFutureListener.CLOSE);
        }
    }

}
