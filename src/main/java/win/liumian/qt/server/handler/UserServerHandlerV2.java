package win.liumian.qt.server.handler;

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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liumian  2021/9/19 07:33
 */
@Slf4j
public class UserServerHandlerV2 extends QuantumCommonHandler {


    private static final Logger logger = LoggerFactory.getLogger(UserServerHandlerV2.class);

    private String userChannelId;

    private String clientId;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        log.info("打开用户channel：" + ctx.channel().id().asLongText());
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
        FullHttpRequest httpRequest = (FullHttpRequest) msg;
        clientId = httpRequest.headers().get("clientId");
        String proxyHost = httpRequest.headers().get("proxyHost");
        String proxyPort = httpRequest.headers().get("proxyPort");

        Channel userChannel = ctx.channel();
        QuantumMessage message = new QuantumMessage();
        message.setClientId(clientId);
        message.setMessageType(QuantumMessageType.DATA);
        userChannelId = userChannel.id().asLongText();
        message.setChannelId(userChannelId);
        message.setProxyHost(proxyHost);
        message.setProxyPort(Integer.parseInt(proxyPort));

        message.setData(toByteArray(httpRequest));

        ChannelMap.userChannelMap.put(userChannelId, userChannel);

        boolean success = writeMessage(message);
        if (!success) {
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

}
