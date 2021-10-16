package win.liumian.qt.server.handler;

import io.netty.handler.codec.http.websocketx.WebSocket00FrameDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.springframework.util.StringUtils;
import win.liumian.qt.common.QuantumMessage;
import win.liumian.qt.common.QuantumMessageType;
import win.liumian.qt.common.handler.QuantumCommonHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liumian.qt.server.channel.ChannelMap;

/**
 * @author liumian  2021/9/19 07:33
 */
@Slf4j
public class UserServerHandler extends QuantumCommonHandler {


    private static final Logger logger = LoggerFactory.getLogger(UserServerHandler.class);

    private String userChannelId;

    private String clientId;

    private String proxyHost;

    private String proxyPort;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        userChannelId = ctx.channel().id().asLongText();
        log.info("打开用户channel：" + userChannelId);
        ChannelMap.userChannelMap.put(userChannelId, ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        String userChannelId = ctx.channel().id().asLongText();
        log.info("关闭用户channel：" + userChannelId);
        ChannelMap.userChannelMap.remove(userChannelId);
        QuantumMessage message = new QuantumMessage();
        message.setClientId(clientId);
        message.setMessageType(QuantumMessageType.USER_DISCONNECTED);
        message.setChannelId(userChannelId);
        writeMessage(message);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        QuantumMessage message = new QuantumMessage();
        byte[] bytes = (byte[]) msg;
        message.setData(bytes);

        if (clientId == null || proxyHost == null || proxyPort == null) {
            String s = new String(bytes);
            clientId = getHeaderValue(s, "clientId");
            proxyHost = getHeaderValue(s, "proxyHost");
            proxyPort = getHeaderValue(s, "proxyPort");
        }

        if (clientId == null || proxyHost == null || proxyPort == null) {
            log.info("缺少参数，clientId={}，proxyHost={}，proxyPort={}", clientId, proxyHost, proxyPort);
            ctx.channel().close();
        }

        message.setClientId(clientId);
        message.setMessageType(QuantumMessageType.DATA);
        message.setChannelId(userChannelId);
        message.setProxyHost(proxyHost);
        message.setProxyPort(Integer.parseInt(proxyPort));


        boolean success = writeMessage(message);
        if (!success) {
            log.info("写入数据失败，clientId={}，proxyHost={}，proxyPort={}", clientId, proxyHost, proxyPort);
            ctx.channel().close();
        }
    }

    private boolean writeMessage(QuantumMessage message) {
        Channel proxyChannel = ChannelMap.proxyChannelsMap.get(clientId);
        if (proxyChannel != null && proxyChannel.isWritable()) {
            String proxyChannelId = proxyChannel.id().asLongText();
            logger.info("用户通道:{} -> 代理通道:{}", userChannelId, proxyChannelId);
            proxyChannel.writeAndFlush(message);
            return true;
        } else {
            return false;
        }
    }

    private byte[] toByteArray(FullHttpRequest httpRequest) {
        EmbeddedChannel ch = new EmbeddedChannel(new HttpRequestEncoder());
        ch.writeOutbound(httpRequest);
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();

        ByteBuf encoded;
        while ((encoded = ch.readOutbound()) != null) {
            buffer.writeBytes(encoded);
        }
        byte[] bytes = ByteBufUtil.getBytes(buffer);
        buffer.release();
        ch.close();
        return bytes;
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
