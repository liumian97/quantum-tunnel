package win.liumian.qt.handler;

import win.liumian.qt.common.QuantumMessage;
import win.liumian.qt.common.QuantumMessageType;
import win.liumian.qt.common.handler.QuantumCommonHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liumian.qt.channel.ChannelMap;

/**
 * @author liumian  2021/9/19 07:33
 */
@Slf4j
public class UserServerHandler extends QuantumCommonHandler {


    private static final Logger logger = LoggerFactory.getLogger(UserServerHandler.class);

    private String userChannelId;

    private String networkId;

    private String targetHost;

    private String targetPort;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        userChannelId = ctx.channel().id().asLongText();
        log.info("打开用户通道：{}", userChannelId);
        ChannelMap.userChannelMap.put(userChannelId, ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        String userChannelId = ctx.channel().id().asLongText();
        log.info("关闭用户通道：{}", userChannelId);
        ChannelMap.userChannelMap.remove(userChannelId);
        QuantumMessage message = new QuantumMessage();
        message.setNetworkId(networkId);
        message.setMessageType(QuantumMessageType.USER_DISCONNECTED);
        message.setChannelId(userChannelId);
        writeMessage(message);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        QuantumMessage message = new QuantumMessage();
        byte[] bytes = (byte[]) msg;
        message.setData(bytes);

        if (networkId == null || targetHost == null || targetPort == null) {
            String s = new String(bytes);
            networkId = getHeaderValue(s, "network_id");
            targetHost = getHeaderValue(s, "target_host");
            targetPort = getHeaderValue(s, "target_port");
            super.networkId = networkId;
        }

        if (networkId == null || targetHost == null || targetPort == null) {
            log.info("缺少参数，networkId={}，targetHost={}，targetPort={}", networkId, targetHost, targetPort);
            ctx.channel().close();
        }

        message.setNetworkId(networkId);
        message.setMessageType(QuantumMessageType.DATA);
        message.setChannelId(userChannelId);
        message.setTargetHost(targetHost);
        message.setTargetPort(Integer.parseInt(targetPort));


        boolean success = writeMessage(message);
        if (!success) {
            log.info("写入数据失败，networkId={}，targetHost={}，targetPort={}", networkId, targetHost, targetPort);
            ctx.channel().close();
        }
    }

    private boolean writeMessage(QuantumMessage message) {
        Channel proxyChannel = ChannelMap.proxyChannelsMap.get(networkId);
        if (proxyChannel != null && proxyChannel.isWritable()) {
            String proxyChannelId = proxyChannel.id().asLongText();
            logger.info("用户通道:{} -> 代理通道:{}", userChannelId, proxyChannelId);
            proxyChannel.writeAndFlush(message);
            return true;
        } else {
            return false;
        }
    }

    private String getHeaderValue(String requestStr, String headerName) {
        for (String s : requestStr.split("\r\n")) {
            if (s.startsWith(headerName + ":")) {
                return s.split(":")[1].trim();
            }
        }
        return null;
    }

}
