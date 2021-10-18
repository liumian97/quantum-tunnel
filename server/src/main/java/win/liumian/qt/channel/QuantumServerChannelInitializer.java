package win.liumian.qt.channel;

import io.netty.handler.codec.bytes.ByteArrayDecoder;
import win.liumian.qt.handler.UserServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;


/**
 * @author liumian  2021/9/18 16:24
 */
public class QuantumServerChannelInitializer extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        //添加编解码
        ChannelPipeline pipeline = socketChannel.pipeline();
        // 请求解码器
        pipeline.addLast(new ByteArrayDecoder(), new ByteArrayEncoder(), new UserServerHandler());

    }
}
