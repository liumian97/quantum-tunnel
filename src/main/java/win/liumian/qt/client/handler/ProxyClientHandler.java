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
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseDecoder;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;

/**
 * @author liumian  2021/9/26 17:13
 */
@Slf4j
public class ProxyClientHandler extends QuantumCommonHandler {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        // register client information
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
        } else if (quantumMessage.getMessageType() == QuantumMessageType.DISCONNECTED) {
            processDisconnected(quantumMessage);
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

    private void processDisconnected(QuantumMessage quantumMessage) {
        log.info("disconnected  quantum-tunnel proxy server");

    }

    private void processData(ChannelHandlerContext ctx, QuantumMessage quantumMessage) {
//        log.info("接收到消息" + new String(quantumMessage.getData()));
        try {
            doProxyRequest(ctx, quantumMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void doProxyRequest(ChannelHandlerContext ctx, QuantumMessage quantumMessage) throws InterruptedException {

        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
//            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws SSLException {
//                    ByteBufAllocator byteBufAllocator = ch.alloc();
//                    //对于每个 SslHandler 实例，都使用 Channel 的 ByteBufAllocator 从 SslContext 获取一个新的 SSLEngine
//                    SSLEngine sslEngine = SslContextBuilder.forClient().build().newEngine(byteBufAllocator);
//                    //服务器端模式，客户端模式设置为true
//                    sslEngine.setUseClientMode(true);
//                    //不需要验证客户端，客户端不设置该项
//                    sslEngine.setNeedClientAuth(false);
//                    //要将 SslHandler 设置为第一个 ChannelHandler。这确保了只有在所有其他的 ChannelHandler 将他们的逻辑应用到数据之后，才会进行加密。
//                    //startTls 如果为true，第一个写入的消息将不会被加密（客户端应该设置为true）
                    ChannelPipeline pipeline = ch.pipeline();
//                    pipeline.addFirst("ssl",new SslHandler(sslEngine));
                    pipeline.addLast("http-decoder", new HttpResponseDecoder())
                            .addLast("http-aggregator", new HttpObjectAggregator(65535))
                            .addLast(new ProxyRequestHandler(ctx, quantumMessage.getChannelId()));
                }
            });
            ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(quantumMessage.getData().length);
            buffer.writeBytes(quantumMessage.getData());
            Channel channel = b.connect(quantumMessage.getProxyHost(), quantumMessage.getProxyPort()).sync().channel();
            channel.writeAndFlush(buffer);
        } catch (Exception e) {
            workerGroup.shutdownGracefully();
            throw e;
        }
    }
}
