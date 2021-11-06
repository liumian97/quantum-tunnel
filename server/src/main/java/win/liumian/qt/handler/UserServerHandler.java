package win.liumian.qt.handler;

import com.google.protobuf.ByteString;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liumian.qt.channel.ChannelMap;
import win.liumian.qt.common.handler.QuantumCommonHandler;
import win.liumian.qt.common.proto.QuantumMessage;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

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

    private SocketAddress remoteAddress;

    public UserServerHandler(String networkId, String targetHost, String targetPort) {
        this.networkId = networkId;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        userChannelId = ctx.channel().id().asLongText();
        remoteAddress = ctx.channel().remoteAddress();
        log.info("打开用户通道，ip：{} ：{}", remoteAddress, userChannelId);
        ChannelMap.userChannelMap.put(userChannelId, ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        String userChannelId = ctx.channel().id().asLongText();
        ChannelMap.userChannelMap.remove(userChannelId);
        log.info("关闭用户通道，ip：{} ：{}", remoteAddress, userChannelId);
        if (networkId != null) {
            //说明从通道中读到了数据，那么通知proxyClient关闭对应的channel
            QuantumMessage.Message message = QuantumMessage.Message.newBuilder().setNetworkId(networkId)
                    .setMessageType(QuantumMessage.MessageType.USER_DISCONNECTED).build();
            writeToProxyChannel(message);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byte[] bytes = (byte[]) msg;
        if (networkId == null || targetHost == null || targetPort == null) {
            String s = new String(bytes);
            networkId = getHeaderValue(s, "network_id");
            if (networkId == null) {
                networkId = getParamValue(s, "network_id");
            }
            targetHost = getHeaderValue(s, "target_host");
            if (targetHost == null) {
                targetHost = getParamValue(s, "target_host");
            }
            targetPort = getHeaderValue(s, "target_port");
            if (targetPort == null) {
                targetPort = getParamValue(s, "target_port");
            }
            super.networkId = networkId;
        }

        if (networkId == null || targetHost == null || targetPort == null) {
            log.info("缺少参数，networkId={}，targetHost={}，targetPort={}", networkId, targetHost, targetPort);
            ctx.channel().close();
        }

        QuantumMessage.Message message = QuantumMessage.Message.newBuilder()
                .setNetworkId(networkId).setMessageType(QuantumMessage.MessageType.DATA)
                .setChannelId(userChannelId).setTargetHost(targetHost)
                .setTargetPort(Integer.parseInt(targetPort))
                .setData(ByteString.copyFrom(new String(bytes), StandardCharsets.UTF_8)).build();


        boolean success = writeToProxyChannel(message);
        if (!success) {
            log.info("写入数据失败，networkId={}，targetHost={}，targetPort={}", networkId, targetHost, targetPort);
            ctx.channel().close();
        }
    }

    private boolean writeToProxyChannel(QuantumMessage.Message message) {
        Channel proxyChannel = ChannelMap.proxyChannelsMap.get(networkId);
        if (proxyChannel != null && proxyChannel.isWritable()) {
            String proxyChannelId = proxyChannel.id().asLongText();
            logger.info("用户通道:{} -> 代理通道:{}", userChannelId, proxyChannelId);
            proxyChannel.writeAndFlush(message);
            return true;
        } else {
            logger.info("代理通道不存在：{}", message.getNetworkId());
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


    private String getParamValue(String requestStr, String paramName) {
        String requestUrl = requestStr.split("\r\n")[0];
        String queryStr = requestUrl.split(" ")[1];
        String[] kv = queryStr.substring(queryStr.indexOf("?") + 1).split("&");
        for (String s : kv) {
            if (s.contains(paramName + "=")) {
                return s.substring(s.indexOf("=") + 1);
            }
        }
        return null;
    }

}
