package win.liumian.qt.channel;

import win.liumian.qt.common.QuantumMessageDecoder;
import win.liumian.qt.common.QuantumMessageEncoder;
import win.liumian.qt.handler.ProxyServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;


/**
 * @author liumian  2021/9/18 16:24
 */
public class ProxyServerChannelInitializer extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(
                new LengthFieldBasedFrameDecoder(65535, 0, 4,0,4),
                new QuantumMessageDecoder(),
                new QuantumMessageEncoder(),
                new ProxyServerHandler());
    }
}
