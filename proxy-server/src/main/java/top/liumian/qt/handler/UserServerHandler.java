package top.liumian.qt.handler;

import com.google.protobuf.ByteString;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.liumian.qt.channel.ChannelMap;
import top.liumian.qt.common.handler.QuantumCommonHandler;
import top.liumian.qt.common.proto.QuantumMessage;

import java.net.SocketAddress;

/**
 * @author liumian  2021/9/19 07:33
 */
@Slf4j
public class UserServerHandler extends QuantumCommonHandler {


    private static final Logger logger = LoggerFactory.getLogger(UserServerHandler.class);

    private String userChannelId;

    private String targetHost;

    private String targetPort;

    private SocketAddress remoteAddress;

    public UserServerHandler(String networkId, String targetHost, String targetPort) {
        super.networkId = networkId;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
    }

    /**
     * 处理用户连接激活事件
     *
     * @param ctx 连接上下文
     * @throws Exception 异常
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        userChannelId = ctx.channel().id().asLongText();
        remoteAddress = ctx.channel().remoteAddress();
        log.info("打开用户通道，ip：{} ：{}", remoteAddress, userChannelId);
        ChannelMap.USER_CHANNEL_MAP.put(userChannelId, ctx.channel());

        if (networkId != null && targetHost != null && targetPort != null) {
            //建立连接时向内网穿透客户端发送一个connect事件，表示正在建立连接。某些协议如MySQL需要该事件
            QuantumMessage.Message message = QuantumMessage.Message.newBuilder().setMessageType(QuantumMessage.MessageType.DATA)
                    .setNetworkId(networkId).setTargetHost(targetHost).setTargetPort(Integer.parseInt(targetPort)).build();
            writeToProxyChannel(message);
        }

    }

    /**
     * 处理用户连接断开时间
     *
     * @param ctx 连接上下文
     * @throws Exception 异常
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        String userChannelId = ctx.channel().id().asLongText();
        ChannelMap.USER_CHANNEL_MAP.remove(userChannelId);
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
        //当代理信息未设置时，尝试从http报文中获取代理配置信息
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
        }

        if (networkId == null || targetHost == null || targetPort == null) {
            log.info("缺少参数，networkId={}，targetHost={}，targetPort={}", networkId, targetHost, targetPort);
            ctx.channel().close();
        }

        QuantumMessage.Message message = QuantumMessage.Message.newBuilder().setNetworkId(networkId)
                .setMessageType(QuantumMessage.MessageType.DATA).setChannelId(userChannelId).setTargetHost(targetHost)
                .setTargetPort(Integer.parseInt(targetPort)).setData(ByteString.copyFrom(bytes)).build();


        boolean success = writeToProxyChannel(message);
        if (!success) {
            log.info("写入数据失败，networkId={}，targetHost={}，targetPort={}", networkId, targetHost, targetPort);
            ctx.channel().close();
        }
    }

    /**
     * 写入到内网穿透客户端
     *
     * @param message 用户数据
     * @return 写入结果
     */
    private boolean writeToProxyChannel(QuantumMessage.Message message) {
        Channel proxyChannel = ChannelMap.PROXY_CHANNEL_MAP.get(networkId);
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

    /**
     * 从http请求报文中获取指定header值
     *
     * @param requestStr http请求报文
     * @param headerName header名称
     * @return header值
     */
    private String getHeaderValue(String requestStr, String headerName) {
        final String lineBreak = "\r\n";
        for (String s : requestStr.split(lineBreak)) {
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
