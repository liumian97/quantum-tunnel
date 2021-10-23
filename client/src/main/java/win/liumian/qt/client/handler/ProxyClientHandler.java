package win.liumian.qt.client.handler;

import com.alibaba.fastjson.JSONObject;
import win.liumian.qt.common.QuantumMessage;
import win.liumian.qt.common.QuantumMessageType;
import win.liumian.qt.common.handler.QuantumCommonHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liumian  2021/9/26 17:13
 */
@Slf4j
public class ProxyClientHandler extends QuantumCommonHandler {

    public final static Map<String, Channel> user2ProxyChannelMap = new ConcurrentHashMap<>();

    private final static NioEventLoopGroup WORKER_GROUP = new NioEventLoopGroup();

    /**
     * 通配符
     */
    private final static String WILD_CARD = "*";


    private String targetServerHost;

    private String targetServerPort;

    public ProxyClientHandler(String networkId, String targetServerHost, String targetServerPort) {
        super.networkId = networkId;
        this.targetServerHost = targetServerHost;
        this.targetServerPort = targetServerPort;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        log.info("准备注册量子通道");
        QuantumMessage quantumMessage = new QuantumMessage();
        quantumMessage.setNetworkId(networkId);
        quantumMessage.setMessageType(QuantumMessageType.REGISTER);
        ctx.writeAndFlush(quantumMessage);
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        QuantumMessage quantumMessage = (QuantumMessage) msg;
        if (quantumMessage.getMessageType() == QuantumMessageType.REGISTER_RESULT) {
            processRegisterResult(ctx, quantumMessage);
        } else if (quantumMessage.getMessageType() == QuantumMessageType.USER_DISCONNECTED) {
            processUserChannelDisconnected(quantumMessage);
        } else if (quantumMessage.getMessageType() == QuantumMessageType.KEEPALIVE) {
            log.info("收到心跳消息，网络id：{}", quantumMessage.getNetworkId());
        } else if (quantumMessage.getMessageType() == QuantumMessageType.DATA) {
            processData(ctx, quantumMessage);
        } else {
            throw new RuntimeException("Unknown type: " + quantumMessage.getMessageType());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("量子通道断开");
    }

    private void processRegisterResult(ChannelHandlerContext ctx, QuantumMessage quantumMessage) {
        JSONObject result = JSONObject.parseObject(new String(quantumMessage.getData()));
        if (result.getBooleanValue("success")) {
            log.info("量子通道注册成功：{}", result);
        } else {
            log.info("量子通道注册失败：{}", result);
            ctx.channel().close();
            throw new RuntimeException("注册失败");
        }
    }

    private void processUserChannelDisconnected(QuantumMessage quantumMessage) {
        Channel channel = user2ProxyChannelMap.get(quantumMessage.getChannelId());
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        log.info("主动关闭代理通道：{}", quantumMessage.getChannelId());
    }

    private void processData(ChannelHandlerContext ctx, QuantumMessage quantumMessage) {
        try {
            doProxyRequest(ctx, quantumMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void doProxyRequest(ChannelHandlerContext ctx, QuantumMessage quantumMessage) throws InterruptedException {
        Channel proxyChannel = user2ProxyChannelMap.get(quantumMessage.getChannelId());
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(quantumMessage.getData().length);
        buffer.writeBytes(quantumMessage.getData());
        if (proxyChannel == null) {
            if (!WILD_CARD.equals(targetServerHost) && !targetServerHost.equals(quantumMessage.getTargetHost())) {
                disconnectUserChannel(ctx, quantumMessage.getChannelId());
                return;
            }

            if (!WILD_CARD.equals(targetServerPort) && Integer.parseInt(targetServerPort) != quantumMessage.getTargetPort()) {
                disconnectUserChannel(ctx, quantumMessage.getChannelId());
                return;
            }
            try {
                Bootstrap b = new Bootstrap();
                b.group(WORKER_GROUP);
                b.channel(NioSocketChannel.class);
                b.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ProxyRequestHandler(ctx, quantumMessage.getChannelId(), networkId));
                    }
                });
                Channel channel = b.connect(quantumMessage.getTargetHost(), quantumMessage.getTargetPort()).sync().channel();
                channel.writeAndFlush(buffer);
            } catch (Exception e) {
                log.error("请求targetServer异常", e);
                //通知服务端proxyChannel已经断开，让其断开userChannel
                disconnectUserChannel(ctx, quantumMessage.getChannelId());
            }
        } else {
            proxyChannel.writeAndFlush(buffer);
        }
    }

    private void disconnectUserChannel(ChannelHandlerContext ctx, String channelId) {
        QuantumMessage message = new QuantumMessage();
        message.setChannelId(channelId);
        message.setMessageType(QuantumMessageType.PROXY_DISCONNECTED);
        ctx.writeAndFlush(message);
    }

}
