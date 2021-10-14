package win.liumian.qt.client.handler;

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

/**
 * @author liumian  2021/9/26 17:13
 */
@Slf4j
public class ProxyClientHandler extends QuantumCommonHandler {



    private final static NioEventLoopGroup WORKER_GROUP = new NioEventLoopGroup();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        log.info("准备注册通道");
        QuantumMessage quantumMessage = new QuantumMessage();
        quantumMessage.setClientId("localTest");
        quantumMessage.setMessageType(QuantumMessageType.REGISTER);
        ctx.writeAndFlush(quantumMessage);

        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        QuantumMessage quantumMessage = (QuantumMessage) msg;
        if (quantumMessage.getMessageType() == QuantumMessageType.REGISTER_RESULT) {
            processRegisterResult(quantumMessage);
        } else if (quantumMessage.getMessageType() == QuantumMessageType.CONNECTED) {
            processConnected(quantumMessage);
        } else if (quantumMessage.getMessageType() == QuantumMessageType.USER_DISCONNECTED) {
            processUserChannelDisconnected(quantumMessage);
        } else if (quantumMessage.getMessageType() == QuantumMessageType.DATA) {
            processData(ctx, quantumMessage);
        } else if (quantumMessage.getMessageType() == QuantumMessageType.KEEPALIVE) {
            // 心跳包, 不处理
        } else {
            throw new RuntimeException("Unknown type: " + quantumMessage.getMessageType());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("loss connection for quantum-tunnel proxy, Please restart!");
    }

    private void processRegisterResult(QuantumMessage quantumMessage) {
        log.info("register to quantum-tunnel proxy server");
    }

    private void processConnected(QuantumMessage quantumMessage) throws Exception {
        log.info("connected to quantum-tunnel proxy server");
    }

    private void processUserChannelDisconnected(QuantumMessage quantumMessage) {
        Channel channel = ProxyRequestHandler.user2ProxyChannelMap.get(quantumMessage.getChannelId());
        if (channel != null && channel.isOpen()){
            channel.close();
        }
        log.info("主动关闭用户代理通道：{}",quantumMessage.getChannelId());
    }

    private void processData(ChannelHandlerContext ctx, QuantumMessage quantumMessage) {
        try {
            doProxyRequest(ctx, quantumMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void doProxyRequest(ChannelHandlerContext ctx, QuantumMessage quantumMessage) throws InterruptedException {
        try {
            Bootstrap b = new Bootstrap();
            b.group(WORKER_GROUP);
            b.channel(NioSocketChannel.class);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new ProxyRequestHandler(ctx, quantumMessage.getChannelId()));
                }
            });
            ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(quantumMessage.getData().length);
            buffer.writeBytes(quantumMessage.getData());
            Channel channel = b.connect(quantumMessage.getProxyHost(), quantumMessage.getProxyPort()).sync().channel();
            channel.writeAndFlush(buffer);
        } catch (Exception e) {
            throw e;
        }
    }
}
